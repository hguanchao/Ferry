package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.DateUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineVersionDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.biz.excel.ExcelExporter;
import com.ferry.devops.biz.excel.PipelineExportRow;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {

    /** 单次导出上限：与列表可选的最大分页一致，再多就不该走页面导出 */
    private static final int EXPORT_LIMIT = 1000;
    private static final String EXPORT_FILE_NAME = "流水线列表";

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
    public void export(PipelineQuery body, List<Long> ids, HttpServletResponse response) {
        // 勾选导出不能直接按 id 查：PipelineMapper 的 xml 里 INNER JOIN 了 dev_app，
        // 数据权限条件挂在应用上，走同一条 SQL 才不会把权限外的应用导出去
        List<PipelineVO> records = baseMapper.selectExportList(body, ids, EXPORT_LIMIT);
        if (CollUtil.isEmpty(records)) {
            throw new ServiceException("当前条件下没有可导出的数据");
        }
        List<PipelineExportRow> rows = records.stream().map(this::toExportRow).toList();
        String operator = Optional.ofNullable(AuthUtil.getUser())
                .map(AuthUser::getNickname)
                .filter(StrUtil::isNotBlank)
                .orElse("未知");
        List<String> watermarks = List.of(
                "导出人：" + operator,
                "导出时间：" + DateUtil.format(new Date(), DateUtil.NORM_DATETIME_PATTERN),
                "筛选条件：" + describeFilters(body, ids),
                "数据量：" + rows.size() + " 条"
        );
        try {
            ExcelExporter.write(response, EXPORT_FILE_NAME, "流水线列表", PipelineExportRow.class, rows, watermarks);
        } catch (IOException e) {
            // 响应流已开始写出，此时再抛给全局处理器也改不了状态码，只能记日志让调用方看到明确的失败
            log.error("流水线导出写出失败", e);
            throw new ServiceException("导出失败，请稍后重试");
        }
    }

    private PipelineExportRow toExportRow(PipelineVO vo) {
        PipelineExportRow row = new PipelineExportRow();
        row.setId(ObjUtil.isEmpty(vo.getId()) ? "" : "PL-" + vo.getId());
        row.setAppCode(vo.getAppCode());
        row.setAppName(vo.getAppName());
        row.setName(vo.getName());
        row.setCurrentVersion(ObjUtil.isEmpty(vo.getCurrentVersionNo()) ? "未配置" : "V" + vo.getCurrentVersionNo());
        row.setStageCount(ObjUtil.isEmpty(vo.getStageCount()) ? "0" : String.valueOf(vo.getStageCount()));
        row.setDefaultBranch(vo.getDefaultBranch());
        row.setOwnerNames(StrUtil.isBlank(vo.getOwnerNames()) ? "" : vo.getOwnerNames().replace(",", "、"));
        row.setDescription(vo.getDescription());
        row.setUpdateTime(ObjUtil.isEmpty(vo.getUpdateTime()) ? ""
                : DateUtil.format(vo.getUpdateTime(), DateUtil.NORM_DATETIME_PATTERN));
        return row;
    }

    /**
     * 把筛选条件拼成一句人话写进导出文件
     *
     * <p>导出文件脱离系统后只剩这一行线索，缺了它拿到表格的人无法判断数据口径。
     */
    private String describeFilters(PipelineQuery body, List<Long> ids) {
        List<String> parts = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            parts.add("勾选导出 " + ids.size() + " 条");
        }
        if (StrUtil.isNotBlank(body.getKeyword())) {
            parts.add("流水线名称包含「" + body.getKeyword() + "」");
        }
        if (CollUtil.isNotEmpty(body.getAppIds())) {
            parts.add("应用 " + body.getAppIds().size() + " 个");
        }
        if (CollUtil.isNotEmpty(body.getOwnerIds())) {
            parts.add("负责人 " + body.getOwnerIds().size() + " 人");
        }
        return parts.isEmpty() ? "无（全量）" : String.join("；", parts);
    }

    private void validateDefinition(PipelineDefinitionDTO definition) {
        if (definition == null) {
            throw new ServiceException("至少配置一个阶段");
        }
        definition.validateStages(definition.getStages());
    }
}
