package com.ferry.devops.biz.service;

import java.util.List;
import java.util.Map;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.entity.PipelineStageTask;

/**
 * 流水线阶段任务服务：负责任务行的读写。
 */
public interface IPipelineStageTaskService extends IService<PipelineStageTask> {

    /**
     * 保存某阶段下的全部任务。
     *
     * @param stageId 阶段 id
     * @param version 阶段定义版本
     * @param tasks   任务定义列表
     */
    void saveTasks(Long stageId, Integer version, List<PipelineTaskDTO> tasks);

    /**
     * 按阶段批量查询任务，并转为 DTO 后按阶段分组。
     *
     * @param stageIds 阶段 id 列表
     * @return 阶段 id -> 任务 DTO 列表
     */
    Map<Long, List<PipelineTaskDTO>> mapTasksByStageIds(List<Long> stageIds);

    /**
     * 删除指定阶段下的全部任务。
     *
     * @param stageIds 阶段 id 列表
     */
    void deleteByStageIds(List<Long> stageIds);
}