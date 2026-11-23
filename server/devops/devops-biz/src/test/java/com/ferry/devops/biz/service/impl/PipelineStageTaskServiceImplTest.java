package com.ferry.devops.biz.service.impl;

import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.PipelineStageTask;
import com.ferry.devops.biz.mapper.PipelineStageTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 阶段任务契约测试：执行器数字码与脚本参数字符串保持不变。 */
@ExtendWith(MockitoExtension.class)
class PipelineStageTaskServiceImplTest {

    private static final long STAGE_ID = 801L;
    private static final String SCRIPT_PARAMS = "{\"artifact\":\"target/*.jar\"}";

    @Mock
    private PipelineStageTaskMapper taskMapper;
    @Captor
    private ArgumentCaptor<Collection<PipelineStageTask>> savedTasks;

    private PipelineStageTaskServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = spy(new PipelineStageTaskServiceImpl());
        Class<?> type = service.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if ("baseMapper".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(service, taskMapper);
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("未找到 baseMapper 字段");
    }

    @Test
    void 保存任务_直接保存执行器编码和脚本参数字符串() {
        PipelineStageTaskDTO task = new PipelineStageTaskDTO();
        task.setName("部署");
        task.setExecutor(2);
        task.setScriptParams(SCRIPT_PARAMS);
        doReturn(true).when(service).saveBatch(anyCollection());

        service.saveTasks(STAGE_ID, 2, List.of(task));

        verify(service).saveBatch(savedTasks.capture());
        PipelineStageTask saved = savedTasks.getValue().iterator().next();
        assertEquals(STAGE_ID, saved.getStageId());
        assertEquals(2, saved.getVersion());
        assertEquals(1, saved.getSort());
        assertEquals(2, saved.getExecutor());
        assertEquals(SCRIPT_PARAMS, saved.getScriptParams());
    }

    @Test
    void 读取任务_不将执行器或脚本参数转换为其他类型() {
        PipelineStageTask task = new PipelineStageTask();
        task.setStageId(STAGE_ID);
        task.setName("部署");
        task.setExecutor(2);
        task.setScriptParams(SCRIPT_PARAMS);
        when(taskMapper.selectByStageIds(List.of(STAGE_ID))).thenReturn(List.of(task));

        PipelineStageTaskDTO result = service.mapTasksByStageIds(List.of(STAGE_ID)).get(STAGE_ID).get(0);

        assertEquals(2, result.getExecutor());
        assertEquals(SCRIPT_PARAMS, result.getScriptParams());
    }
}
