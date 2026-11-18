package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.RollbackDTO;
import com.ferry.devops.api.dto.query.RollbackQuery;
import com.ferry.devops.api.entity.DeployRecord;
import com.ferry.devops.api.entity.Environment;
import com.ferry.devops.api.entity.RollbackRecord;
import com.ferry.devops.api.enums.EnvCategoryEnum;
import com.ferry.devops.api.vo.RollbackCandidateVO;
import com.ferry.devops.api.vo.RollbackVO;
import com.ferry.devops.biz.integration.deployer.Deployer;
import com.ferry.devops.biz.integration.deployer.DeployerFactory;
import com.ferry.devops.biz.mapper.DeployLockMapper;
import com.ferry.devops.biz.mapper.DeployRecordMapper;
import com.ferry.devops.biz.mapper.EnvironmentMapper;
import com.ferry.devops.biz.mapper.RollbackRecordMapper;
import com.ferry.devops.biz.service.IRollbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 回滚服务实现。
 *
 * <p>只列健康版本（prod_use_status=1/2）；生产回滚需不同确认人（Controller :prod 权限 + 服务端比较）。
 */
@Service
@RequiredArgsConstructor
public class RollbackServiceImpl extends ServiceImpl<RollbackRecordMapper, RollbackRecord> implements IRollbackService {

    private final EnvironmentMapper environmentMapper;
    private final DeployRecordMapper deployRecordMapper;
    private final DeployLockMapper lockMapper;
    private final DeployerFactory deployerFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(RollbackDTO body) {
        Environment env = environmentMapper.selectById(body.getEnvId());
        if (env == null) {
            throw new ServiceException("环境不存在");
        }
        Long operator = AuthUtil.getUserId();
        if (ObjUtil.equals(env.getCategory(), EnvCategoryEnum.PRODUCTION.getCode())) {
            if (body.getConfirmer() == null) {
                throw new ServiceException("生产回滚需选择确认人");
            }
            if (ObjUtil.equals(operator, body.getConfirmer())) {
                throw new ServiceException("确认人不能与操作人相同");
            }
        }
        // 再次校验环境空闲与制品存在/健康
        if (lockMapper.countActive(env.getAppId(), env.getId()) > IntConst.ZERO) {
            throw new ServiceException("环境正在部署中，不可回滚");
        }
        Map<String, Object> artifact = deployRecordMapper.selectArtifactInfo(body.getArtifactId());
        if (artifact == null || !healthy(artifact)) {
            throw new ServiceException("回滚目标制品不存在或不是健康版本");
        }

        Deployer deployer = deployerFactory.get(env.getDeployType());
        Deployer.DeployContext ctx = new Deployer.DeployContext();
        ctx.setEnvId(body.getEnvId());
        ctx.setArtifactId(body.getArtifactId());
        ctx.setArtifactDigest(String.valueOf(artifact.get("digest")));
        Deployer.RollbackTarget target = new Deployer.RollbackTarget();
        target.setEnvId(body.getEnvId());
        target.setArtifactId(body.getArtifactId());
        target.setArtifactDigest(String.valueOf(artifact.get("digest")));
        deployer.rollback(ctx, target);

        RollbackRecord record = new RollbackRecord();
        record.setDeployId(body.getDeployId());
        record.setEnvId(body.getEnvId());
        record.setArtifactId(body.getArtifactId());
        record.setCurrentArtifactId(env.getCurrentArtifactId());
        record.setReason(body.getReason());
        record.setOperator(operator);
        record.setConfirmer(body.getConfirmer());
        record.setStartTime(new Date());
        record.setResult(IntConst.ONE);
        save(record);
        // 更新环境当前生效制品
        env.setCurrentArtifactId(body.getArtifactId());
        environmentMapper.updateById(env);
        record.setEndTime(new Date());
        record.setDuration(record.getEndTime().getTime() - record.getStartTime().getTime());
        updateById(record);
        return record.getId();
    }

    @Override
    public IPage<RollbackVO> pageList(RollbackQuery body) {
        IPage<RollbackRecord> pages = page(Page.create(body), Wrappers.<RollbackRecord>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(body.getEnvId()), RollbackRecord::getEnvId, body.getEnvId())
                .eq(ObjUtil.isNotEmpty(body.getResult()), RollbackRecord::getResult, body.getResult())
                .ge(ObjUtil.isNotEmpty(body.getStartTimeStart()), RollbackRecord::getStartTime, body.getStartTimeStart())
                .le(ObjUtil.isNotEmpty(body.getStartTimeEnd()), RollbackRecord::getStartTime, body.getStartTimeEnd())
                .orderByDesc(RollbackRecord::getCreateTime));
        List<RollbackVO> records = new ArrayList<>();
        for (RollbackRecord record : pages.getRecords()) {
            RollbackVO vo = BeanUtil.copy(record, RollbackVO.class);
            Map<String, Object> info = baseMapper.selectEnvArtifact(record.getEnvId(), record.getArtifactId());
            if (info != null) {
                vo.setEnvName(String.valueOf(info.get("envName")));
                vo.setArtifactVersion(String.valueOf(info.get("artifactVersion")));
            }
            records.add(vo);
        }
        IPage<RollbackVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public List<RollbackCandidateVO> candidates(Long envId) {
        Environment env = environmentMapper.selectById(envId);
        if (env == null) {
            throw new ServiceException("环境不存在");
        }
        List<Map<String, Object>> rows = baseMapper.selectHealthyCandidates(env.getAppId());
        List<RollbackCandidateVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            RollbackCandidateVO vo = new RollbackCandidateVO();
            Long artifactId = ((Number) row.get("artifactId")).longValue();
            vo.setArtifactId(artifactId);
            vo.setVersion(String.valueOf(row.get("version")));
            vo.setDigest(String.valueOf(row.get("digest")));
            int useStatus = ((Number) row.get("prodUseStatus")).intValue();
            vo.setProdUseStatus(useStatus);
            vo.setDeployedAt((Date) row.get("createTime"));
            vo.setLabel(useStatus == IntConst.ONE ? "最近健康版本（当前生效）"
                    : useStatus == 2 ? "历史健康版本" : "健康版本");
            result.add(vo);
        }
        return result;
    }

    private boolean healthy(Map<String, Object> artifact) {
        Object status = artifact.get("prodUseStatus");
        if (!(status instanceof Number)) {
            return false;
        }
        int s = ((Number) status).intValue();
        return s == 1 || s == 2;
    }
}
