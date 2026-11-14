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
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.excel.PipelineExportRow;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineVersionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {
    private final IAppService appService;
    private final IPipelineVersionService pipelineVersionService;

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

        Integer stageCount = IntConst.ZERO;
        if (ObjUtil.isNotEmpty(body.getId())) {
            Pipeline pipeline = getById(body.getId());
            if (ObjUtil.isEmpty(pipeline)) {
                throw new ServiceException("流水线不存在");
            }
            if (!ObjUtil.equals(pipeline.getAppId(), body.getAppId())) {
                throw new ServiceException("流水线所属应用创建后不可修改");
            }
            if (ObjUtil.isNotEmpty(body.getDefinition())) {
                throw new ServiceException("定义变更请使用版本保存接口");
            }
            stageCount = pipeline.getStageCount();
        }
        Pipeline pipeline = BeanUtil.copy(body, Pipeline.class);
        pipeline.setStageCount(stageCount);
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

    private void validateDefinition(PipelineDefinitionDTO definition) {
        if (definition == null) {
            throw new ServiceException("至少配置一个阶段");
        }
        definition.validateStages(definition.getStages());
    }
}
