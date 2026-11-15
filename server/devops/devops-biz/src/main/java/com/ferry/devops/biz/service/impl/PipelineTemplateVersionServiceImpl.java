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
import com.ferry.devops.api.dto.PipelineTemplateDefinitionDTO;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import com.ferry.devops.api.vo.PipelineTemplateDefinitionVO;
import com.ferry.devops.api.vo.PipelineTemplateVersionVO;
import com.ferry.devops.biz.mapper.PipelineTemplateVersionMapper;
import com.ferry.devops.biz.service.IPipelineTemplateVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineTemplateVersionServiceImpl extends ServiceImpl<PipelineTemplateVersionMapper, PipelineTemplateVersion>
        implements IPipelineTemplateVersionService {
    private static final String STAGES = "stages";
    private static final String VARIABLES = "variables";

    @Override
    public List<PipelineTemplateVersionVO> list(Long templateId) {
        List<PipelineTemplateVersion> list = list(Wrappers.<PipelineTemplateVersion>lambdaQuery()
                .eq(PipelineTemplateVersion::getTemplateId, templateId).orderByDesc(PipelineTemplateVersion::getVersionNo));
        return BeanUtil.copy(list, PipelineTemplateVersionVO.class);
    }

    @Override
    public PipelineTemplateVersionVO detail(Long id) {
        PipelineTemplateVersion version = getById(id);
        if (ObjUtil.isEmpty(version)) {
            throw new ServiceException("版本不存在");
        }
        PipelineTemplateVersionVO vo = BeanUtil.copy(version, PipelineTemplateVersionVO.class);
        vo.setDefinition(JsonUtil.toBean(version.getContent(), PipelineTemplateDefinitionVO.class));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineTemplateVersionDTO body) {
        PipelineTemplate template = baseMapper.selectTemplateInfo(body.getTemplateId());
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("模板不存在");
        }
        validateDefinition(body.getDefinition());

        String content = JsonUtil.toJson(body.getDefinition());
        String contentHash = CryptoUtil.sha256Hex(content);
        if (template.getCurrentVersionId() != null) {
            PipelineTemplateVersion current = getById(template.getCurrentVersionId());
            // 定义没变就不产出空版本，只保留最近一次真实变更
            if (ObjUtil.isNotEmpty(current) && ObjUtil.equals(current.getContentHash(), contentHash)) {
                return true;
            }
        }

        int versionNo = nextVersionNo(template);
        String changeNote = body.getChangeNote();
        if (StrUtil.isBlank(changeNote)) {
            if (template.getCurrentVersionId() != null) {
                throw new ServiceException("请填写变更说明");
            }
            changeNote = "初始版本";
        }

        PipelineTemplateVersion version = new PipelineTemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setContentHash(contentHash);
        version.setChangeNote(changeNote);
        version.setCreateUserName(currentUserName());
        boolean isSuccess = save(version);
        if (isSuccess) {
            // 阶段数与占位变量数从本次保存的快照统计并回写模板，保证口径一致
            JsonNode stages = JsonUtil.getNode(content, STAGES);
            JsonNode variables = JsonUtil.getNode(content, VARIABLES);
            baseMapper.updateTemplate(template.getId(), version.getId(), versionNo,
                    stages == null ? IntConst.ZERO : stages.size(),
                    variables == null ? IntConst.ZERO : variables.size());
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(PipelineTemplateVersionDTO body) {
        PipelineTemplate template = baseMapper.selectTemplateInfo(body.getTemplateId());
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("模板不存在");
        }
        PipelineTemplateVersion source = getById(body.getVersionId());
        if (ObjUtil.isEmpty(source) || !ObjUtil.equals(source.getTemplateId(), template.getId())) {
            throw new ServiceException("版本不存在");
        }
        if (ObjUtil.equals(template.getCurrentVersionId(), source.getId())) {
            throw new ServiceException("该版本已是当前版本");
        }

        int versionNo = nextVersionNo(template);
        PipelineTemplateVersion version = new PipelineTemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersionNo(versionNo);
        version.setContent(source.getContent());
        version.setContentHash(source.getContentHash());
        version.setChangeNote("回退自 V" + source.getVersionNo());
        version.setCreateUserName(currentUserName());
        boolean isSuccess = save(version);
        if (isSuccess) {
            // 阶段数与占位变量数按源版本快照统计，不取回退请求里的定义
            JsonNode stages = JsonUtil.getNode(source.getContent(), STAGES);
            JsonNode variables = JsonUtil.getNode(source.getContent(), VARIABLES);
            baseMapper.updateTemplate(template.getId(), version.getId(), versionNo,
                    stages == null ? IntConst.ZERO : stages.size(),
                    variables == null ? IntConst.ZERO : variables.size());
        }
        return isSuccess;
    }

    /**
     * 当前登录人名称快照，优先昵称
     *
     * @return 名称，未登录时返回 null
     */
    private String currentUserName() {
        AuthUser user = AuthUtil.getUser();
        if (ObjUtil.isEmpty(user)) {
            return null;
        }
        return StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getAccount();
    }

    /**
     * 校验模板定义
     *
     * @param definition 模板定义
     */
    private void validateDefinition(PipelineTemplateDefinitionDTO definition) {
        if (definition == null) {
            throw new ServiceException("至少配置一个阶段");
        }
        definition.validate();
    }

    /**
     * 计算下一个版本号
     *
     * @param template 模板
     * @return 当前版本号加一，首个版本为 1
     */
    private int nextVersionNo(PipelineTemplate template) {
        return (template.getCurrentVersionNo() == null ? 0 : template.getCurrentVersionNo()) + 1;
    }
}
