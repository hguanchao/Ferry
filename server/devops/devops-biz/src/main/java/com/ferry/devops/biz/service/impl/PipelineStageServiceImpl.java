package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.PipelineStage;
import com.ferry.devops.api.entity.PipelineStageTask;
import com.ferry.devops.api.vo.PipelineStageTaskVO;
import com.ferry.devops.api.vo.PipelineStageVO;
import com.ferry.devops.biz.mapper.PipelineStageMapper;
import com.ferry.devops.biz.service.IPipelineStageService;
import com.ferry.devops.biz.service.IPipelineStageTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流水线阶段服务实现：管理阶段记录，并通过阶段任务服务装配任务。
 */
@Service
@RequiredArgsConstructor
public class PipelineStageServiceImpl extends ServiceImpl<PipelineStageMapper, PipelineStage> implements IPipelineStageService {
    private final IPipelineStageTaskService pipelineStageTaskService;

    @Override
    public List<PipelineStageVO> list(PipelineStageDTO body) {
        Long pipelineId = body.getPipelineId();
        Integer version = body.getVersion();
        List<PipelineStage> stageList = list(Wrappers.<PipelineStage>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(pipelineId), PipelineStage::getPipelineId, pipelineId)
                .eq(ObjUtil.isNotEmpty(version), PipelineStage::getVersion, version)
                .orderByAsc(PipelineStage::getSort));
        if (CollUtil.isEmpty(stageList)) {
            return Collections.emptyList();
        }
        List<PipelineStageVO> stageVOList = BeanUtil.copy(stageList, PipelineStageVO.class);
        Set<Long> stageIds = stageVOList.stream().map(PipelineStageVO::getId).collect(Collectors.toSet());
        List<PipelineStageTask> stageTaskList = pipelineStageTaskService.list(Wrappers.<PipelineStageTask>lambdaQuery()
                .in(PipelineStageTask::getStageId, stageIds));
        if (CollUtil.isNotEmpty(stageTaskList)) {
            Map<Long, List<PipelineStageTaskVO>> stageTaskMap = stageTaskList.stream().collect(Collectors.groupingBy(
                    PipelineStageTask::getStageId,
                    Collectors.mapping(task -> BeanUtil.copy(task, PipelineStageTaskVO.class), Collectors.toList())
            ));
            for (PipelineStageVO stageVO : stageVOList) {
                stageVO.setTaskList(stageTaskMap.get(stageVO.getId()));
            }
        }
        return stageVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDefinition(Long pipelineId, Integer version, PipelineDefinitionDTO definition) {
        List<PipelineStageDTO> stages = definition == null ? null : definition.getStages();
        if (stages == null) {
            return;
        }
        int sort = 1;
        for (PipelineStageDTO stage : stages) {
            PipelineStage entity = new PipelineStage();
            entity.setPipelineId(pipelineId);
            entity.setVersion(version);
            entity.setType(stage.getType());
            entity.setName(stage.getName());
            entity.setBuildTool(stage.getBuildTool());
            entity.setTimeout(stage.getTimeout());
            entity.setOutputs(stage.getOutputs());
            entity.setSort(sort++);
            save(entity);
            pipelineStageTaskService.saveTasks(entity.getId(), version, stage.getTasks());
        }
    }

    @Override
    public PipelineDefinitionDTO loadDefinition(Long pipelineId, Integer version) {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        List<PipelineStage> stages = baseMapper.selectByPipelineVersion(pipelineId, version);
        List<PipelineStageDTO> stageDtos = new ArrayList<>();
        if (!stages.isEmpty()) {
            List<Long> stageIds = stages.stream().map(PipelineStage::getId).toList();
            Map<Long, List<PipelineStageTaskDTO>> tasksByStage =
                    pipelineStageTaskService.mapTasksByStageIds(stageIds);
            for (PipelineStage stage : stages) {
                PipelineStageDTO stageDto = new PipelineStageDTO();
                stageDto.setType(stage.getType());
                stageDto.setName(stage.getName());
                stageDto.setBuildTool(stage.getBuildTool());
                stageDto.setTimeout(stage.getTimeout());
                stageDto.setOutputs(stage.getOutputs());
                stageDto.setTasks(tasksByStage.getOrDefault(stage.getId(), List.of()));
                stageDtos.add(stageDto);
            }
        }
        definition.setStages(stageDtos);
        return definition;
    }

    @Override
    public String loadDefinitionJson(Long pipelineId, Integer version) {
        return JsonUtil.toJson(loadDefinition(pipelineId, version));
    }

    @Override
    public List<Integer> listVersions(Long pipelineId) {
        return baseMapper.selectDistinctVersions(pipelineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPipeline(Long pipelineId) {
        List<PipelineStage> stages = list(Wrappers.<PipelineStage>lambdaQuery()
                .eq(PipelineStage::getPipelineId, pipelineId));
        if (stages.isEmpty()) {
            return;
        }
        List<Long> stageIds = stages.stream().map(PipelineStage::getId).collect(Collectors.toList());
        pipelineStageTaskService.deleteByStageIds(stageIds);
        removeByIds(stageIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(List<PipelineStageDTO> bodys) {
        if (CollUtil.isEmpty(bodys)) {
            return true;
        }
        for (PipelineStageDTO body : bodys) {
            PipelineStage entity = BeanUtil.copy(body, PipelineStage.class);
            boolean isSuccess = save(entity);
            if (isSuccess) {
                int taskSort = IntConst.ZERO;
                List<PipelineStageTaskDTO> tasks = body.getTasks();
                for (PipelineStageTaskDTO task : tasks) {
                    task.setStageId(entity.getId());
                    task.setVersion(entity.getVersion());
                    task.setSort(taskSort);
                    taskSort++;
                }
                pipelineStageTaskService.saveBatch(BeanUtil.copy(tasks, PipelineStageTask.class));
            }
        }
        return true;
    }

    @Override
    public boolean deleted(List<Long> pipelineIds) {
        if (CollUtil.isEmpty(pipelineIds)) {
            return true;
        }
        List<Long> ids = listObjs(Wrappers.<PipelineStage>lambdaQuery().select(PipelineStage::getId)
                .in(PipelineStage::getPipelineId, pipelineIds));
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            pipelineStageTaskService.remove(Wrappers.<PipelineStageTask>lambdaQuery()
                    .in(PipelineStageTask::getStageId, ids));
        }
        return isSuccess;
    }
}
