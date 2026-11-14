package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineVersionDTO;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineVersionVO;
import com.ferry.devops.biz.mapper.PipelineVersionMapper;
import com.ferry.devops.biz.service.IPipelineVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineVersionServiceImpl extends ServiceImpl<PipelineVersionMapper, PipelineVersion> implements IPipelineVersionService {
    private static final String STAGES = "stages";

    @Override
    public List<PipelineVersionVO> list(Long pipelineId) {
        List<PipelineVersion> list = list(Wrappers.<PipelineVersion>lambdaQuery()
                .eq(PipelineVersion::getPipelineId, pipelineId).orderByDesc(PipelineVersion::getVersionNo));
        return BeanUtil.copy(list, PipelineVersionVO.class);
    }

    @Override
    public PipelineVersionVO detail(Long id) {
        PipelineVersion version = getById(id);
        if (ObjUtil.isEmpty(version)) {
            throw new ServiceException("版本不存在");
        }
        PipelineVersionVO vo = BeanUtil.copy(version, PipelineVersionVO.class);
        vo.setDefinition(JsonUtil.toBean(version.getContent(), PipelineDefinitionVO.class));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineVersionDTO body) {
        Pipeline pipeline = baseMapper.selectPipelineInfo(body.getPipelineId());
        if (ObjUtil.isEmpty(pipeline)) {
            throw new ServiceException("流水线不存在");
        }
        validateDefinition(body.getDefinition());

        String content = JsonUtil.toJson(body.getDefinition());
        String contentHash = CryptoUtil.sha256Hex(content);
        if (pipeline.getCurrentVersionId() != null) {
            PipelineVersion current = getById(pipeline.getCurrentVersionId());
            // 定义没变就不产出空版本，只保留最近一次真实变更
            if (ObjUtil.isNotEmpty(current) && ObjUtil.equals(current.getContentHash(), contentHash)) {
                return true;
            }
        }

        int versionNo = nextVersionNo(pipeline);
        String changeNote = body.getChangeNote();
        if (StrUtil.isBlank(changeNote)) {
            if (pipeline.getCurrentVersionId() != null) {
                throw new ServiceException("请填写变更说明");
            }
            changeNote = "初始版本";
        }

        PipelineVersion version = new PipelineVersion();
        version.setPipelineId(pipeline.getId());
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setContentHash(contentHash);
        version.setChangeNote(changeNote);
        version.setCreateUserName(currentUserName());
        boolean isSuccess = save(version);
        if (isSuccess) {
            // 阶段数从本次保存的快照统计并回写流水线，保证口径一致
            JsonNode stages = JsonUtil.getNode(content, STAGES);
            baseMapper.updatePipeline(pipeline.getId(), version.getId(), versionNo, stages == null ? null : stages.size());
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(PipelineVersionDTO body) {
        Pipeline pipeline = baseMapper.selectPipelineInfo(body.getPipelineId());
        if (ObjUtil.isEmpty(pipeline)) {
            throw new ServiceException("流水线不存在");
        }
        PipelineVersion source = getById(body.getVersionId());
        if (ObjUtil.isEmpty(source) || !ObjUtil.equals(source.getPipelineId(), pipeline.getId())) {
            throw new ServiceException("版本不存在");
        }
        if (ObjUtil.equals(pipeline.getCurrentVersionId(), source.getId())) {
            throw new ServiceException("该版本已是当前版本");
        }

        int versionNo = nextVersionNo(pipeline);
        PipelineVersion version = new PipelineVersion();
        version.setPipelineId(pipeline.getId());
        version.setVersionNo(versionNo);
        version.setContent(source.getContent());
        version.setContentHash(source.getContentHash());
        version.setChangeNote("回退自 V" + source.getVersionNo());
        version.setCreateUserName(currentUserName());
        boolean isSuccess = save(version);
        if (isSuccess) {
            // 阶段数按源版本快照统计，不取回退请求里的定义
            JsonNode stages = JsonUtil.getNode(source.getContent(), STAGES);
            baseMapper.updatePipeline(pipeline.getId(), version.getId(), versionNo, stages == null ? null : stages.size());
        }
        return isSuccess;
    }

    private String currentUserName() {
        AuthUser user = AuthUtil.getUser();
        if (ObjUtil.isEmpty(user)) {
            return null;
        }
        return StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getAccount();
    }

    private void validateDefinition(PipelineDefinitionDTO definition) {
        if (definition == null) {
            throw new ServiceException("至少配置一个阶段");
        }
        definition.validateStages(definition.getStages());
    }

    private int nextVersionNo(Pipeline pipeline) {
        return (pipeline.getCurrentVersionNo() == null ? 0 : pipeline.getCurrentVersionNo()) + 1;
    }
}
