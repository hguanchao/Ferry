package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.ArtifactDTO;
import com.ferry.devops.api.dto.ArtifactPromoteDTO;
import com.ferry.devops.api.dto.query.ArtifactQuery;
import com.ferry.devops.api.entity.Artifact;
import com.ferry.devops.api.entity.ArtifactEnv;
import com.ferry.devops.api.enums.ArtifactFormEnum;
import com.ferry.devops.api.enums.DeployTypeEnum;
import com.ferry.devops.api.enums.EnvCategoryEnum;
import com.ferry.devops.api.enums.GateStatusEnum;
import com.ferry.devops.api.vo.ArtifactDetailVO;
import com.ferry.devops.api.vo.ArtifactEnvVO;
import com.ferry.devops.api.vo.ArtifactVO;
import com.ferry.devops.api.vo.PromotionPrecheckVO;
import com.ferry.devops.api.vo.TraceVO;
import com.ferry.devops.biz.integration.storage.S3Client;
import com.ferry.devops.biz.mapper.ArtifactEnvMapper;
import com.ferry.devops.biz.mapper.ArtifactMapper;
import com.ferry.devops.biz.service.IArtifactService;
import com.ferry.devops.biz.wrapper.ArtifactWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 制品服务实现：入库复核、列表追溯、晋级、软删除恢复。
 */
@Service
@RequiredArgsConstructor
public class ArtifactServiceImpl extends ServiceImpl<ArtifactMapper, Artifact> implements IArtifactService {

    private final ArtifactEnvMapper envMapper;
    private final S3Client s3Client;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean intake(ArtifactDTO body) {
        if (baseMapper.countByVersion(body.getAppId(), body.getVersion()) > IntConst.ZERO) {
            throw new ServiceException("同版本制品已存在，不可覆盖");
        }
        if (!s3Client.digestExists(body.getDigest())) {
            throw new ServiceException("摘要反查失败，制品入库失败");
        }
        Artifact artifact = BeanUtil.copy(body, Artifact.class);
        artifact.setProdUseStatus(IntConst.THREE);
        artifact.setConfirmStatus(IntConst.ONE);
        artifact.setDeleteSeq(0L);
        artifact.setGateConclusion(body.getGateConclusion());
        if (ObjUtil.isEmpty(artifact.getBuilder())) {
            artifact.setBuilder(AuthUtil.getUserId());
        }
        if (save(artifact)) {
            markEnv(artifact.getId(), artifact.getAppId(), null, null, EnvCategoryEnum.TEST.getCode());
            return true;
        }
        return false;
    }

    @Override
    public IPage<ArtifactVO> pageList(ArtifactQuery body) {
        ArtifactWrapper wrapper = ArtifactWrapper.build();
        IPage<Artifact> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<ArtifactVO> result = wrapper.pageVo(pages);
        for (ArtifactVO vo : result.getRecords()) {
            vo.setEnvTags(envMapper.categories(vo.getId()));
        }
        return result;
    }

    @Override
    public ArtifactDetailVO detail(Long id) {
        Artifact artifact = require(id);
        ArtifactDetailVO vo = BeanUtil.copy(artifact, ArtifactDetailVO.class);
        vo.setEnvTags(envMapper.categories(id));
        List<ArtifactEnv> envs = envMapper.selectList(Wrappers.<ArtifactEnv>lambdaQuery()
                .eq(ArtifactEnv::getArtifactId, id).orderByAsc(ArtifactEnv::getPromotedAt));
        vo.setEnvs(BeanUtil.copy(envs, ArtifactEnvVO.class));
        vo.setPromoteTimeline(envs.stream().map(env -> {
            ArtifactDetailVO.TimelineItem item = new ArtifactDetailVO.TimelineItem();
            item.setEnvId(env.getEnvId());
            item.setEnvCategory(env.getEnvCategory());
            item.setOperator(env.getPromoter());
            item.setTime(env.getPromotedAt());
            item.setResult(env.getSourceEnvId() == null ? "入库" : "晋级");
            return item;
        }).collect(Collectors.toList()));
        vo.setDeployTimeline(baseMapper.deployTimeline(id).stream().map(this::timeline).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public TraceVO trace(Long id) {
        require(id);
        TraceVO vo = baseMapper.traceBase(id);
        if (vo == null) {
            throw new ServiceException("制品不存在");
        }
        vo.setReleaseIds(baseMapper.traceReleaseIds(id));
        vo.setDeployIds(baseMapper.traceDeployIds(id));
        vo.setRollbackIds(baseMapper.traceRollbackIds(id));
        return vo;
    }

    @Override
    public List<ArtifactVO> search(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            throw new ServiceException("请输入摘要、版本号或 Commit");
        }
        ArtifactWrapper wrapper = ArtifactWrapper.build();
        return wrapper.listVo(baseMapper.search(keyword));
    }

    @Override
    public PromotionPrecheckVO promotePrecheck(Long id, Long targetEnvId) {
        Artifact artifact = require(id);
        Map<String, Object> env = requireEnv(targetEnvId);
        Integer targetCategory = toInt(env.get("category"));
        Integer deployType = toInt(env.get("deployType"));

        PromotionPrecheckVO vo = new PromotionPrecheckVO();
        vo.setTargetEnvId(targetEnvId);
        vo.setGateConclusion(artifact.getGateConclusion());
        List<PromotionPrecheckVO.Item> items = new ArrayList<>();

        // 门禁：全部通过（1）或不适用（3）/已豁免（4）视为通过，失败/获取失败阻塞
        boolean gateOk = ObjUtil.isEmpty(artifact.getGateConclusion())
                || ObjUtil.equals(artifact.getGateConclusion(), GateStatusEnum.PASS.getCode())
                || ObjUtil.equals(artifact.getGateConclusion(), GateStatusEnum.NOT_APPLICABLE.getCode())
                || ObjUtil.equals(artifact.getGateConclusion(), GateStatusEnum.EXEMPTED.getCode());
        vo.setHasExemption(ObjUtil.equals(artifact.getGateConclusion(), GateStatusEnum.EXEMPTED.getCode()));
        items.add(item("门禁结论", gateOk, gateOk ? "通过" : "门禁未通过或数据获取失败，禁止晋级"));

        // 形态匹配
        boolean formOk = formMatches(artifact.getForm(), deployType);
        vo.setFormMatched(formOk);
        items.add(item("形态匹配", formOk, formOk ? "通过" : "制品形态与目标环境部署类型不匹配"));

        // 来源环境链：测试(1)→预发(2)→生产(3)
        ArtifactEnv source = null;
        if (ObjUtil.equals(targetCategory, EnvCategoryEnum.STAGING.getCode())) {
            source = baseMapper.latestEnv(id, EnvCategoryEnum.TEST.getCode());
            items.add(item("来源环境", source != null, source != null ? "测试→预发" : "未晋级到测试环境"));
        } else if (ObjUtil.equals(targetCategory, EnvCategoryEnum.PRODUCTION.getCode())) {
            source = baseMapper.latestEnv(id, EnvCategoryEnum.STAGING.getCode());
            items.add(item("来源环境", source != null, source != null ? "预发→生产" : "生产仅接受预发来源"));
        } else {
            items.add(item("目标环境", false, "只能晋级到预发或生产环境"));
        }
        Long sourceEnvId = source == null ? null : source.getEnvId();
        vo.setSourceEnvId(sourceEnvId);

        // 已晋级校验：不允许重复晋级同一环境
        if (baseMapper.countEnv(id, targetEnvId) > IntConst.ZERO) {
            items.add(item("重复晋级", false, "制品已晋级到该环境"));
        }

        // 生产豁免备注提示由前端根据 hasExemption 处理
        boolean can = items.stream().allMatch(PromotionPrecheckVO.Item::getPass);
        vo.setItems(items);
        vo.setCanPromote(can);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean promote(ArtifactPromoteDTO body) {
        PromotionPrecheckVO pre = promotePrecheck(body.getId(), body.getTargetEnvId());
        if (!Boolean.TRUE.equals(pre.getCanPromote())) {
            String reason = pre.getItems().stream().filter(i -> !Boolean.TRUE.equals(i.getPass()))
                    .map(PromotionPrecheckVO.Item::getDetail).collect(Collectors.joining("；"));
            throw new ServiceException("晋级校验失败：" + reason);
        }
        Map<String, Object> env = requireEnv(body.getTargetEnvId());
        Integer category = toInt(env.get("category"));
        if (ObjUtil.equals(category, EnvCategoryEnum.PRODUCTION.getCode()) && StrUtil.isBlank(body.getNote())) {
            throw new ServiceException("晋级到生产必须填写备注说明");
        }
        // 只写环境标记，不改制品、不改摘要、不复制对象
        markEnv(body.getId(), null, body.getTargetEnvId(), pre.getSourceEnvId(), category);
        // 更新目标环境当前生效制品
        baseMapper.updateEnvCurrentArtifact(body.getTargetEnvId(), body.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restore(Long id) {
        Artifact artifact = getById(id);
        if (artifact == null || ObjUtil.equals(artifact.getDeleteSeq(), 0L)) {
            throw new ServiceException("制品不存在或未删除");
        }
        if (artifact.getDeletedAt() == null
                || System.currentTimeMillis() - artifact.getDeletedAt().getTime() > 30L * 24 * 60 * 60 * 1000) {
            throw new ServiceException("超过 30 天恢复期，不可恢复");
        }
        if (baseMapper.countByVersion(artifact.getAppId(), artifact.getVersion()) > IntConst.ZERO) {
            throw new ServiceException("同版本已有有效制品，不可恢复");
        }
        artifact.setDeleteSeq(0L);
        artifact.setDeletedAt(null);
        artifact.setDeleteSource(null);
        return updateById(artifact);
    }

    private void markEnv(Long artifactId, Long appIdForTest, Long envId, Long sourceEnvId, Integer category) {
        Long targetEnvId = envId;
        if (targetEnvId == null && appIdForTest != null) {
            targetEnvId = baseMapper.testEnvId(appIdForTest);
        }
        if (targetEnvId == null) {
            return;
        }
        ArtifactEnv env = new ArtifactEnv();
        env.setArtifactId(artifactId);
        env.setEnvId(targetEnvId);
        env.setEnvCategory(category);
        env.setSourceEnvId(sourceEnvId);
        env.setPromoter(AuthUtil.getUserId());
        env.setPromotedAt(new Date());
        envMapper.insert(env);
    }

    private Artifact require(Long id) {
        Artifact artifact = getById(id);
        if (artifact == null || !ObjUtil.equals(artifact.getDeleteSeq(), 0L)) {
            throw new ServiceException("制品不存在");
        }
        return artifact;
    }

    private Map<String, Object> requireEnv(Long envId) {
        Map<String, Object> env = baseMapper.envInfo(envId);
        if (env == null) {
            throw new ServiceException("目标环境不存在");
        }
        return env;
    }

    private boolean formMatches(Integer form, Integer deployType) {
        if (ObjUtil.equals(form, ArtifactFormEnum.PACKAGE.getCode())) {
            return ObjUtil.equals(deployType, DeployTypeEnum.SSH.getCode());
        }
        return ObjUtil.equals(form, ArtifactFormEnum.IMAGE.getCode())
                && (ObjUtil.equals(deployType, DeployTypeEnum.DOCKER.getCode())
                || ObjUtil.equals(deployType, DeployTypeEnum.KUBERNETES.getCode()));
    }

    private PromotionPrecheckVO.Item item(String name, boolean pass, String detail) {
        PromotionPrecheckVO.Item item = new PromotionPrecheckVO.Item();
        item.setName(name);
        item.setPass(pass);
        item.setDetail(detail);
        return item;
    }

    private ArtifactDetailVO.TimelineItem timeline(Map<String, Object> row) {
        ArtifactDetailVO.TimelineItem item = new ArtifactDetailVO.TimelineItem();
        item.setEnvId(toLong(row.get("envId")));
        item.setEnvCategory(toInt(row.get("envCategory")));
        item.setOperator(toLong(row.get("operator")));
        Object time = row.get("time");
        if (time instanceof Date date) {
            item.setTime(date);
        }
        item.setResult(ObjUtil.isEmpty(row.get("result")) ? null : String.valueOf(row.get("result")));
        return item;
    }

    private Integer toInt(Object value) {
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }
}
