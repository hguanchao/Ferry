package com.ferry.devops.biz.service.impl;

import java.util.Date;
import java.util.List;

import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunOperation;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.config.DevopsProperties;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunOperationMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 运行状态服务测试：单测/扫描成功进入待门禁、人工继续判断。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunStageServiceImplTest {

    private static final Long RUN_ID = 100L;
    private static final Long STAGE_ID = 200L;

    @Mock
    private RunMapper runMapper;
    @Mock
    private RunStageMapper stageMapper;
    @Mock
    private RunStageTaskMapper taskMapper;
    @Mock
    private RunOperationMapper operationMapper;
    @Mock
    private RunStageGateMapper gateResultMapper;

    private RunStageServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Run.class);
        DevopsProperties properties = new DevopsProperties();
        properties.getGate().setEnabled(true);
        service = new RunStageServiceImpl(runMapper, stageMapper, taskMapper, operationMapper,
                gateResultMapper, properties);
    }

    private Run runningRun() {
        Run run = new Run();
        run.setId(RUN_ID);
        run.setStatus(RunStatusEnum.RUNNING.getCode());
        return run;
    }

    private RunStage stage(int type, Integer status) {
        RunStage stage = new RunStage();
        stage.setId(STAGE_ID);
        stage.setRunId(RUN_ID);
        stage.setType(type);
        stage.setStatus(status);
        stage.setSort(1);
        return stage;
    }

    @Test
    void 单测阶段成功_进入待门禁而不是直接成功() {
        RunStage stage = stage(2, StageStatusEnum.NOT_STARTED.getCode());
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(runningRun());
        when(stageMapper.selectByRun(RUN_ID)).thenReturn(List.of(stage));
        when(gateResultMapper.countBlocking(RUN_ID)).thenReturn(0L);
        when(operationMapper.selectLatestByRun(RUN_ID)).thenReturn(null);
        when(taskMapper.selectByStage(STAGE_ID)).thenReturn(List.of());

        service.onStageSucceeded(RUN_ID, STAGE_ID, new Date());

        ArgumentCaptor<RunStage> captor = ArgumentCaptor.forClass(RunStage.class);
        verify(stageMapper).updateById(captor.capture());
        assertEquals(StageStatusEnum.PENDING_GATE.getCode(), captor.getValue().getStatus());
    }

    @Test
    void 构建阶段成功_直接成功() {
        RunStage stage = stage(1, StageStatusEnum.RUNNING.getCode());
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(runningRun());
        when(stageMapper.selectByRun(RUN_ID)).thenReturn(List.of(stage));
        when(gateResultMapper.countBlocking(RUN_ID)).thenReturn(0L);
        when(operationMapper.selectLatestByRun(RUN_ID)).thenReturn(null);

        service.onStageSucceeded(RUN_ID, STAGE_ID, new Date());

        ArgumentCaptor<RunStage> captor = ArgumentCaptor.forClass(RunStage.class);
        verify(stageMapper).updateById(captor.capture());
        assertEquals(StageStatusEnum.SUCCESS.getCode(), captor.getValue().getStatus());
    }

    @Test
    void 门禁阻塞的运行不能人工继续() {
        Run run = runningRun();
        run.setStatus(RunStatusEnum.BLOCKED.getCode());
        when(runMapper.selectById(RUN_ID)).thenReturn(run);
        when(gateResultMapper.countBlocking(RUN_ID)).thenReturn(1L);
        RunOperation pause = new RunOperation();
        pause.setType(1);
        when(operationMapper.selectLatestByRun(RUN_ID)).thenReturn(pause);

        assertFalse(service.canResumeManually(RUN_ID));
    }

    @Test
    void 人工暂停且无门禁阻塞_可以继续() {
        Run run = runningRun();
        run.setStatus(RunStatusEnum.BLOCKED.getCode());
        when(runMapper.selectById(RUN_ID)).thenReturn(run);
        when(gateResultMapper.countBlocking(RUN_ID)).thenReturn(0L);
        when(stageMapper.selectByRun(RUN_ID)).thenReturn(List.of(stage(1, StageStatusEnum.SUCCESS.getCode())));
        RunOperation pause = new RunOperation();
        pause.setType(1);
        when(operationMapper.selectLatestByRun(RUN_ID)).thenReturn(pause);

        assertTrue(service.canResumeManually(RUN_ID));
    }
}
