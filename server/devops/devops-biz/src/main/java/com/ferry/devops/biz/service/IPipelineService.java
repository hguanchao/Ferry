package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelinePreviewVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.ValidateResultVO;
import com.ferry.devops.api.vo.VersionVO;

import java.util.List;

/**
 * 流水线服务：保存后经由同步编排创建/更新 Jenkins Job（保存即同步），
 * 触发侧凭流水线状态拦截未同步脚本。
 */
public interface IPipelineService extends IService<Pipeline> {

    IPage<PipelineVO> pageList(PipelineQuery body);

    PipelineVO detail(Long id);

    /**
     * 新增或编辑（元数据 + 阶段定义单一保存入口），保存后同步 Jenkins Job。
     *
     * @param body 流水线表单；stages 为 null 表示不修改定义，空数组表示显式清空旧脚本
     * @return 响应文案；同步失败时为「已保存但同步失败，可重试」的提示
     */
    String save(PipelineDTO body);

    String deleted(List<Long> ids);

    /**
     * 启用并同步 Job（禁用位清除）。
     *
     * @return 响应文案；同步失败时状态已保存为同步失败，可重试
     */
    String enable(Long id);

    /**
     * 停用并同步 Job（禁用位置位）。
     *
     * @return 响应文案；停用同步失败时保持停用态（安全态），Job 禁用位由下次同步补齐
     */
    String disable(Long id);

    /**
     * Jenkins Job 同步重试入口。
     *
     * @param id 流水线 id
     * @return 响应文案（同步成功/失败原因）
     */
    String sync(Long id);

    PipelineDefinitionVO definition(Long id, Long versionId);

    ValidateResultVO validateDefinition(String definition, Integer mode);

    /**
     * 按当前定义生成 Jenkins 脚本预览（与 Job 同步脚本同源）。
     *
     * @param definition 流水线定义 JSON
     * @param mode       1 表单 2 YAML（不支持）
     * @return 脚本与告警
     */
    PipelinePreviewVO previewDefinition(String definition, Integer mode);

    VersionVO saveDefinition(Long id, String definition, Integer mode, String changeNote);

    IPage<VersionVO> versionPage(Long pipelineId, Long current, Long size);

    DiffVO versionDiff(Long leftId, Long rightId);

    VersionVO rollback(Long id, Long versionId, String changeNote);

    IPage<TriggerRecordVO> triggerRecords(Long appId, Long current, Long size);
}
