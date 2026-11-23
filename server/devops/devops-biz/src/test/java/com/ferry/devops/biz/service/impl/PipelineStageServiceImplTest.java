package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.PipelineStage;
import com.ferry.devops.api.entity.PipelineStageTask;
import com.ferry.devops.api.vo.PipelineStageTaskVO;
import com.ferry.devops.api.vo.PipelineStageVO;
import com.ferry.devops.biz.mapper.PipelineStageMapper;
import com.ferry.devops.biz.service.IPipelineStageTaskService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 阶段表字段契约测试：BeanUtil 装配与字符串产出物保存、读取。 */
@ExtendWith(MockitoExtension.class)
class PipelineStageServiceImplTest {

    private static final long PIPELINE_ID = 701L;
    private static final long STAGE_ID = 801L;
    private static final long TASK_ID = 901L;
    private static final int VERSION = 2;
    private static final String OUTPUTS = "target/*.jar, dist/**";
    private static final String SCRIPT_PARAMS = "{\"artifact\":\"target/*.jar\"}";

    @Mock
    private PipelineStageMapper stageMapper;
    @Mock
    private IPipelineStageTaskService taskService;

    private PipelineStageServiceImpl service;

    @BeforeAll
    static void initTableMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PipelineStage.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PipelineStageTask.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new PipelineStageServiceImpl(taskService);
        Class<?> type = service.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if ("baseMapper".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(service, stageMapper);
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("未找到 baseMapper 字段");
    }

    @Test
    void 阶段列表_按表字段类型复制并组装任务() {
        PipelineStage stage = new PipelineStage();
        stage.setId(STAGE_ID);
        stage.setPipelineId(PIPELINE_ID);
        stage.setVersion(VERSION);
        stage.setType(4);
        stage.setName("制品入库");
        stage.setTimeout(600);
        stage.setOutputs(OUTPUTS);
        stage.setSort(1);
        PipelineStageTask task = new PipelineStageTask();
        task.setId(TASK_ID);
        task.setStageId(STAGE_ID);
        task.setVersion(VERSION);
        task.setName("归档");
        task.setExecutor(1);
        task.setScriptParams(SCRIPT_PARAMS);
        task.setSort(1);
        when(stageMapper.selectList(any())).thenReturn(List.of(stage));
        when(taskService.list(ArgumentMatchers.<Wrapper<PipelineStageTask>>any())).thenReturn(List.of(task));
        PipelineStageDTO query = new PipelineStageDTO();
        query.setPipelineId(PIPELINE_ID);
        query.setVersion(VERSION);

        PipelineStageVO result = service.list(query).get(0);
        PipelineStageTaskVO resultTask = result.getTaskList().get(0);

        assertEquals(STAGE_ID, result.getId());
        assertEquals(PIPELINE_ID, result.getPipelineId());
        assertEquals(VERSION, result.getVersion());
        assertEquals(600, result.getTimeout());
        assertEquals(OUTPUTS, result.getOutputs());
        assertEquals(TASK_ID, resultTask.getId());
        assertEquals(STAGE_ID, resultTask.getStageId());
        assertEquals(VERSION, resultTask.getVersion());
        assertEquals(1, resultTask.getExecutor());
        assertEquals(1, resultTask.getSort());
        assertEquals(SCRIPT_PARAMS, resultTask.getScriptParams());
    }

    @Test
    void 保存读取定义_产出物与脚本参数保持字符串() {
        PipelineStageTaskDTO task = new PipelineStageTaskDTO();
        task.setName("归档");
        task.setExecutor(1);
        task.setScriptParams(SCRIPT_PARAMS);
        PipelineStageDTO stage = new PipelineStageDTO();
        stage.setType(4);
        stage.setName("制品入库");
        stage.setTimeout(600);
        stage.setOutputs(OUTPUTS);
        stage.setTasks(List.of(task));
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        definition.setStages(List.of(stage));
        when(stageMapper.insert(any(PipelineStage.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PipelineStage.class).setId(STAGE_ID);
            return 1;
        });

        service.saveDefinition(PIPELINE_ID, VERSION, definition);

        ArgumentCaptor<PipelineStage> savedStage = ArgumentCaptor.forClass(PipelineStage.class);
        verify(stageMapper).insert(savedStage.capture());
        assertEquals(OUTPUTS, savedStage.getValue().getOutputs());
        assertEquals(600, savedStage.getValue().getTimeout());
        verify(taskService).saveTasks(STAGE_ID, VERSION, stage.getTasks());
        when(stageMapper.selectByPipelineVersion(PIPELINE_ID, VERSION)).thenReturn(List.of(savedStage.getValue()));
        when(taskService.mapTasksByStageIds(List.of(STAGE_ID))).thenReturn(Map.of(STAGE_ID, List.of(task)));

        PipelineStageDTO loaded = service.loadDefinition(PIPELINE_ID, VERSION).getStages().get(0);

        assertEquals(OUTPUTS, loaded.getOutputs());
        assertEquals(600, loaded.getTimeout());
        assertEquals(SCRIPT_PARAMS, loaded.getTasks().get(0).getScriptParams());
    }

    @Test
    void 阶段列表为空_返回空列表且不查询任务() {
        when(stageMapper.selectList(any())).thenReturn(List.of());

        assertTrue(service.list(new PipelineStageDTO()).isEmpty());
        verify(taskService, never()).list(ArgumentMatchers.<Wrapper<PipelineStageTask>>any());
    }

}
