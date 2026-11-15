package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.DateUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.PoiUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineVersionDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.excel.PipelineExportRow;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineTemplateService;
import com.ferry.devops.biz.service.IPipelineVersionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {
    private final IAppService appService;
    private final IPipelineVersionService pipelineVersionService;
    private final IPipelineTemplateService pipelineTemplateService;

    @Override
    public IPage<PipelineVO> pageList(PipelineQuery body) {
        return baseMapper.selectPageList(Page.create(body), body);
    }

    @Override
    public PipelineVO detail(Long id) {
        Pipeline pipeline = getById(id);
        if (ObjUtil.isEmpty(pipeline)) {
            throw new ServiceException("流水线不存在");
        }
        // 流水线本身没有团队字段，用应用的数据权限确认可见性，同时取列表所需的展示字段
        App app = appService.getById(pipeline.getAppId());
        if (ObjUtil.isEmpty(app)) {
            throw new ServiceException("应用不存在");
        }
        PipelineVO vo = BeanUtil.copy(pipeline, PipelineVO.class);
        vo.setAppCode(app.getCode());
        vo.setAppName(app.getName());
        vo.setDefaultBranch(app.getRepoBranch());
        vo.setOwnerNames(app.getOwnerNames());
        if (ObjUtil.isNotEmpty(pipeline.getCurrentVersionId())) {
            PipelineVersion version = pipelineVersionService.getById(pipeline.getCurrentVersionId());
            if (ObjUtil.isEmpty(version)) {
                throw new ServiceException("当前版本不存在，请联系管理员");
            }
            vo.setDefinition(JsonUtil.toBean(version.getContent(), PipelineDefinitionVO.class));
        }
        if (ObjUtil.isNotEmpty(pipeline.getSourceTemplateId())) {
            PipelineTemplate template = pipelineTemplateService.getById(pipeline.getSourceTemplateId());
            if (ObjUtil.isNotEmpty(template)) {
                vo.setSourceTemplateName(template.getName());
                // 取值 JSON 与视图字段类型不同，BeanUtil 不会拷贝，这里手动解析回填
                vo.setSourceTemplateVars(parseVars(pipeline.getSourceTemplateVars()));
                Integer derivedNo = pipeline.getSourceTemplateVersionNo() == null
                        ? IntConst.ZERO : pipeline.getSourceTemplateVersionNo();
                vo.setSourceTemplateOutdated(template.getCurrentVersionNo() != null && template.getCurrentVersionNo() > derivedNo);
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineDTO body) {
        App app = appService.getById(body.getAppId());
        if (ObjUtil.isEmpty(app)) {
            throw new ServiceException("应用不存在");
        }
        if (!IntConst.ONE.equals(app.getStatus())) {
            throw new ServiceException("应用已停用，不能创建流水线");
        }
        long count = count(Wrappers.<Pipeline>lambdaQuery().eq(Pipeline::getAppId, app.getId())
                .eq(Pipeline::getName, body.getName()).ne(ObjUtil.isNotEmpty(body.getId()), Pipeline::getId, body.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("同一应用下流水线名称已存在");
        }
        validateDefinition(body.getDefinition());

        Pipeline pipeline = BeanUtil.copy(body, Pipeline.class);
        pipeline.setStageCount(IntConst.ZERO);
        if (ObjUtil.isEmpty(body.getId())) {
            applySourceTemplate(body, pipeline);
        } else {
            Pipeline existing = getById(body.getId());
            if (ObjUtil.isEmpty(existing)) {
                throw new ServiceException("流水线不存在");
            }
            if (!ObjUtil.equals(existing.getAppId(), body.getAppId())) {
                throw new ServiceException("流水线所属应用创建后不可修改");
            }
            if (ObjUtil.isNotEmpty(body.getDefinition())) {
                throw new ServiceException("定义变更请使用版本保存接口");
            }
            pipeline.setStageCount(existing.getStageCount());
            // 编辑不改动派生来源，请求里携带的相关字段一律忽略
            pipeline.setSourceTemplateId(existing.getSourceTemplateId());
            pipeline.setSourceTemplateVersionNo(existing.getSourceTemplateVersionNo());
            pipeline.setSourceTemplateVars(existing.getSourceTemplateVars());
            pipeline.setSourceTemplateMergedTime(existing.getSourceTemplateMergedTime());
        }
        boolean isSuccess = saveOrUpdate(pipeline);
        if (isSuccess) {
            PipelineVersionDTO versionDTO = new PipelineVersionDTO();
            versionDTO.setPipelineId(pipeline.getId());
            versionDTO.setDefinition(body.getDefinition());
            versionDTO.setChangeNote(body.getChangeNote());
            pipelineVersionService.save(versionDTO);
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的流水线");
        }
        return removeByIds(ids);
    }

    @Override
    public void export(PipelineQuery body, HttpServletResponse response) {
        body.setSize(-1);
        IPage<PipelineVO> page = pageList(body);
        try {
            List<PipelineExportRow> rows = new ArrayList<>();
            for (PipelineVO record : page.getRecords()) {
                PipelineExportRow row = new PipelineExportRow();
                row.setId(StrUtil.toStr(record.getId()));
                row.setAppCode(record.getAppCode());
                row.setAppName(record.getAppName());
                row.setName(record.getName());
                Integer currentVersionNo = record.getCurrentVersionNo();
                row.setCurrentVersion(ObjUtil.isEmpty(currentVersionNo) ? "未配置" : "V" + currentVersionNo);
                Integer stageCount = record.getStageCount();
                row.setStageCount(ObjUtil.isEmpty(stageCount) ? "0" : String.valueOf(stageCount));
                row.setDefaultBranch(record.getDefaultBranch());
                if (StrUtil.isNotBlank(record.getOwnerNames())) {
                    row.setOwnerNames(record.getOwnerNames().replace(StrConst.COMMA, StrConst.SLASH));
                }
                row.setDescription(record.getDescription());
                if (ObjUtil.isNotEmpty(record.getUpdateTime())) {
                    row.setUpdateTime(DateUtil.format(record.getUpdateTime(), DateUtil.NORM_DATETIME_PATTERN));
                }
                rows.add(row);
            }
            PoiUtil.exports("流水线列表", rows, PipelineExportRow.class, response);
        } catch (IOException e) {
            // 响应流已开始写出，状态码无法再改，记日志后抛业务异常
            log.error("流水线导出写出失败", e);
            throw new ServiceException("导出失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean templateAck(Long pipelineId, Boolean merged) {
        if (pipelineId == null) {
            throw new ServiceException("流水线id不能为空");
        }
        if (merged == null) {
            throw new ServiceException("确认类型不能为空");
        }
        Pipeline pipeline = getById(pipelineId);
        if (ObjUtil.isEmpty(pipeline)) {
            throw new ServiceException("流水线不存在");
        }
        if (ObjUtil.isEmpty(pipeline.getSourceTemplateId())) {
            throw new ServiceException("该流水线不是从模板派生，无需确认");
        }
        PipelineTemplate template = pipelineTemplateService.getById(pipeline.getSourceTemplateId());
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("来源模板不存在");
        }
        if (template.getCurrentVersionNo() == null) {
            throw new ServiceException("来源模板还没有可用版本");
        }
        // 推进记录的模板版本号即视为已跟进；合并时额外记录合并时间
        return lambdaUpdate()
                .eq(Pipeline::getId, pipeline.getId())
                .set(Pipeline::getSourceTemplateVersionNo, template.getCurrentVersionNo())
                .set(Boolean.TRUE.equals(merged), Pipeline::getSourceTemplateMergedTime, new Date())
                .update();
    }

    private void validateDefinition(PipelineDefinitionDTO definition) {
        if (definition == null) {
            throw new ServiceException("至少配置一个阶段");
        }
        definition.validateStages(definition.getStages());
    }

    /**
     * 处理新增时的模板派生来源：校验模板可用并写入来源字段，派生为独立副本、不保留引用
     *
     * @param body     表单数据
     * @param pipeline 待落库的流水线
     */
    private void applySourceTemplate(PipelineDTO body, Pipeline pipeline) {
        if (ObjUtil.isEmpty(body.getSourceTemplateId())) {
            return;
        }
        PipelineTemplate template = pipelineTemplateService.getById(body.getSourceTemplateId());
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("来源模板不存在");
        }
        if (!IntConst.ONE.equals(template.getStatus())) {
            throw new ServiceException("模板已停用，不能从它创建流水线");
        }
        Map<String, String> vars = body.getSourceTemplateVars();
        if (CollUtil.isNotEmpty(vars)) {
            if (vars.size() > 20) {
                throw new ServiceException("占位变量取值不能超过 20 项");
            }
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                if (StrUtil.isBlank(entry.getKey())) {
                    throw new ServiceException("占位变量名不能为空");
                }
                if (entry.getValue() != null && entry.getValue().length() > 500) {
                    throw new ServiceException("占位变量「" + entry.getKey() + "」的取值不能超过 500 个字符");
                }
            }
        }
        pipeline.setSourceTemplateId(template.getId());
        pipeline.setSourceTemplateVersionNo(body.getSourceTemplateVersionNo() != null
                ? body.getSourceTemplateVersionNo() : template.getCurrentVersionNo());
        pipeline.setSourceTemplateVars(CollUtil.isEmpty(vars) ? null : JsonUtil.toJson(vars));
    }

    /**
     * 解析派生时的占位变量取值 JSON
     *
     * @param json 取值 JSON
     * @return 变量取值，无记录时返回 null
     */
    private Map<String, String> parseVars(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        Map<String, String> vars = new LinkedHashMap<>();
        JsonNode node = JsonUtil.readTree(json);
        node.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value != null && value.isString()) {
                vars.put(entry.getKey(), value.asString());
            }
        });
        return vars;
    }
}
