package com.ferry.devops.api.dto;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.devops.api.enums.PipelineStageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流水线定义
 */
@Data
public class PipelineDefinitionDTO {

    @Schema(description = "阶段列表，数组顺序即执行顺序")
    @Valid
    private List<PipelineStageDTO> stages;

    public void validateStages(List<PipelineStageDTO> stages) {
        if (CollUtil.isEmpty(stages)) {
            throw new ServiceException("至少配置一个阶段");
        }
        if (stages.size() > 20) {
            throw new ServiceException("阶段数量不能超过 " + 20 + " 个");
        }
        Set<String> stageTypes = new HashSet<>();
        for (PipelineStageDTO stage : stages) {
            if (stage == null) {
                throw new ServiceException("阶段配置不能为空");
            }
            if (!PipelineStageTypeEnum.contains(stage.getType())) {
                throw new ServiceException("阶段类型不合法：" + stage.getType());
            }
            if (!stageTypes.add(stage.getType())) {
                throw new ServiceException("阶段类型「" + PipelineStageTypeEnum.of(stage.getType()).getDesc() + "」重复");
            }
            validateStageTasks(stage);
            if (stage.getArtifacts() != null && stage.getArtifacts().size() > 20) {
                throw new ServiceException("阶段「" + stage.getName() + "」的产出物声明不能超过 " + 20 + " 个");
            }
        }
    }

    public void validateStageTasks(PipelineStageDTO stage) {
        if (CollUtil.isEmpty(stage.getTasks())) {
            return;
        }
        if (stage.getTasks().size() > 30) {
            throw new ServiceException("阶段「" + stage.getName() + "」的任务不能超过 " + 30 + " 个");
        }
        Set<String> taskNames = new HashSet<>();
        for (PipelineTaskDTO task : stage.getTasks()) {
            if (task == null) {
                throw new ServiceException("任务配置不能为空");
            }
            // 前后空格不构成不同的任务，按去空格后的名称判重
            String taskName = task.getName() == null ? "" : task.getName().trim();
            if (!taskNames.add(taskName)) {
                throw new ServiceException("阶段「" + stage.getName() + "」内任务名称重复");
            }
        }
    }
}
