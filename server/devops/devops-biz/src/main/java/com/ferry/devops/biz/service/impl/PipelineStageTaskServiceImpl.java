package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.PipelineStageTask;
import com.ferry.devops.biz.mapper.PipelineStageTaskMapper;
import com.ferry.devops.biz.service.IPipelineStageTaskService;
import org.springframework.stereotype.Service;

/**
 * 流水线阶段任务服务实现。
 *
 * <p>任务字段与表结构一致，executor 使用数字码，scriptParams 保留 JSON 字符串。
 */
@Service
public class PipelineStageTaskServiceImpl extends ServiceImpl<PipelineStageTaskMapper, PipelineStageTask>
        implements IPipelineStageTaskService {

    @Override
    public void saveTasks(Long stageId, Integer version, List<PipelineStageTaskDTO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<PipelineStageTask> entities = new ArrayList<>();
        int sort = 1;
        for (PipelineStageTaskDTO task : tasks) {
            PipelineStageTask entity = new PipelineStageTask();
            entity.setStageId(stageId);
            entity.setVersion(version);
            entity.setName(task.getName());
            entity.setExecutor(task.getExecutor() == null ? IntConst.ONE : task.getExecutor());
            entity.setScriptRef(task.getScriptRef());
            entity.setScriptContent(task.getScriptContent());
            entity.setScriptParams(task.getScriptParams());
            entity.setSort(sort++);
            entities.add(entity);
        }
        saveBatch(entities);
    }

    @Override
    public Map<Long, List<PipelineStageTaskDTO>> mapTasksByStageIds(List<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PipelineStageTaskDTO>> result = new HashMap<>();
        for (PipelineStageTask task : baseMapper.selectByStageIds(stageIds)) {
            result.computeIfAbsent(task.getStageId(), key -> new ArrayList<>()).add(toTaskDto(task));
        }
        return result;
    }

    @Override
    public void deleteByStageIds(List<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return;
        }
        remove(Wrappers.<PipelineStageTask>lambdaQuery()
                .in(PipelineStageTask::getStageId, stageIds));
    }

    private PipelineStageTaskDTO toTaskDto(PipelineStageTask task) {
        return BeanUtil.copy(task, PipelineStageTaskDTO.class);
    }
}