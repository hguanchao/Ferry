package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.http.R;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunTask;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunTaskMapper;
import com.ferry.devops.biz.service.IGateResultService;
import com.ferry.devops.biz.service.IRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Jenkins 状态回调测试：CI 终态收敛、失败跳过、运行态上报、终态幂等与验签拒绝。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JenkinsWebhookControllerTest {

    private static final Long RUN_ID = 2124027461130596353L;

    @Mock
    private RunMapper runMapper;
    @Mock
    private RunStageMapper stageMapper;
    @Mock
    private RunTaskMapper taskMapper;
    @Mock
    private IRunService runService;
    @Mock
    private IGateResultService gateResultService;

    private JenkinsWebhookController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new JenkinsWebhookController(runMapper, stageMapper, taskMapper, runService, gateResultService);
        setSecret("");
    }

    private void setSecret(String value) throws Exception {
        Field field = JenkinsWebhookController.class.getDeclaredField("callbackSecret");
        field.setAccessible(true);
        field.set(controller, value);
    }

    private Run runningRun() {
        Run run = new Run();
        run.setId(RUN_ID);
        run.setStatus(RunStatusEnum.RUNNING.getCode());
        run.setStartTime(new Date());
        return run;
    }

    private RunStage stage(long id, int type, int seq, Integer status) {
        RunStage stage = new RunStage();
        stage.setId(id);
        stage.setRunId(RUN_ID);
        stage.setStageType(type);
        stage.setSeq(seq);
        stage.setStatus(status);
        return stage;
    }

    private RunTask task(long id, long stageId, Integer status) {
        RunTask task = new RunTask();
        task.setId(id);
        task.setRunId(RUN_ID);
        task.setStageId(stageId);
        task.setStatus(status);
        return task;
    }

    /** 构建完成后的阶段快照：CI 四阶段给定状态，部署/发布域按给定状态 */
    private List<RunStage> fullRun(int ci2Status, int ci3Status, int ci4Status, Integer nonCiStatus) {
        return List.of(
                stage(201, 1, 1, StageStatusEnum.SUCCESS.getCode()),
                stage(202, 2, 2, ci2Status),
                stage(203, 3, 3, ci3Status),
                stage(204, 4, 4, ci4Status),
                stage(205, 5, 5, nonCiStatus),
                stage(206, 6, 6, nonCiStatus),
                stage(207, 7, 7, nonCiStatus),
                stage(208, 8, 8, nonCiStatus));
    }

    @Test
    void 最后一个CI阶段成功回调_运行收敛为成功_非CI未开始阶段跳过() {
        Run run = runningRun();
        when(runMapper.selectById(RUN_ID)).thenReturn(run);
        when(stageMapper.selectById(202L)).thenReturn(stage(202, 2, 2, StageStatusEnum.NOT_STARTED.getCode()));
        when(stageMapper.selectByRun(RUN_ID)).thenReturn(
                fullRun(StageStatusEnum.SUCCESS.getCode(), StageStatusEnum.SUCCESS.getCode(),
                        StageStatusEnum.SUCCESS.getCode(), StageStatusEnum.NOT_STARTED.getCode()));
        when(taskMapper.selectByStage(202L)).thenReturn(
                List.of(task(302, 202, StageStatusEnum.NOT_STARTED.getCode())));

        R<String> resp = controller.status(RUN_ID, 202L, 4L, "SUCCESS", null, null, null);

        assertTrue(resp.isSuccess());
        ArgumentCaptor<Run> runCaptor = ArgumentCaptor.forClass(Run.class);
        verify(runMapper).updateById(runCaptor.capture());
        assertEquals(RunStatusEnum.SUCCESS.getCode(), runCaptor.getValue().getStatus());
        assertNotNull(runCaptor.getValue().getEndTime());
        assertNotNull(runCaptor.getValue().getDuration());
        // 1 个回调阶段更新 + 4 个非 CI 未开始阶段置跳过
        ArgumentCaptor<RunStage> stageCaptor = ArgumentCaptor.forClass(RunStage.class);
        verify(stageMapper, times(5)).updateById(stageCaptor.capture());
        assertEquals(4, stageCaptor.getAllValues().stream()
                .filter(s -> StageStatusEnum.SKIPPED.getCode().equals(s.getStatus())).count());
        // 回调镜像：回调阶段的任务同步置成功，不再等轮询
        ArgumentCaptor<RunTask> taskCaptor = ArgumentCaptor.forClass(RunTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertEquals(3, taskCaptor.getValue().getStatus());
    }

    @Test
    void CI阶段失败回调_后续未开始阶段全部跳过_运行置失败() {
        Run run = runningRun();
        when(runMapper.selectById(RUN_ID)).thenReturn(run);
        when(stageMapper.selectById(202L)).thenReturn(stage(202, 2, 2, StageStatusEnum.NOT_STARTED.getCode()));
        when(stageMapper.selectByRun(RUN_ID)).thenReturn(
                fullRun(StageStatusEnum.FAILURE.getCode(), StageStatusEnum.NOT_STARTED.getCode(),
                        StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.NOT_STARTED.getCode()));

        R<String> resp = controller.status(RUN_ID, 202L, 4L, "FAILURE", null, null, null);

        assertTrue(resp.isSuccess());
        ArgumentCaptor<Run> runCaptor = ArgumentCaptor.forClass(Run.class);
        verify(runMapper).updateById(runCaptor.capture());
        assertEquals(RunStatusEnum.FAILURE.getCode(), runCaptor.getValue().getStatus());
        assertNotNull(runCaptor.getValue().getEndTime());
        // seq>2 的 6 个未开始阶段（CI 2 个 + 非 CI 4 个）跳过 + 1 个回调阶段更新
        ArgumentCaptor<RunStage> stageCaptor = ArgumentCaptor.forClass(RunStage.class);
        verify(stageMapper, times(7)).updateById(stageCaptor.capture());
        assertEquals(6, stageCaptor.getAllValues().stream()
                .filter(s -> StageStatusEnum.SKIPPED.getCode().equals(s.getStatus())).count());
    }

    @Test
    void 运行态回调_阶段与运行置进行中() {
        Run run = new Run();
        run.setId(RUN_ID);
        run.setStatus(RunStatusEnum.QUEUED.getCode());
        when(runMapper.selectById(RUN_ID)).thenReturn(run);
        when(stageMapper.selectById(201L)).thenReturn(stage(201, 1, 1, StageStatusEnum.NOT_STARTED.getCode()));

        R<String> resp = controller.status(RUN_ID, 201L, null, "RUNNING", null, null, null);

        assertTrue(resp.isSuccess());
        ArgumentCaptor<Run> runCaptor = ArgumentCaptor.forClass(Run.class);
        verify(runMapper).updateById(runCaptor.capture());
        assertEquals(RunStatusEnum.RUNNING.getCode(), runCaptor.getValue().getStatus());
        assertNotNull(runCaptor.getValue().getStartTime());
    }

    @Test
    void 阶段已终态的重复回调_幂等忽略不再推进() {
        when(runMapper.selectById(RUN_ID)).thenReturn(runningRun());
        when(stageMapper.selectById(201L)).thenReturn(stage(201, 1, 1, StageStatusEnum.SUCCESS.getCode()));

        R<String> resp = controller.status(RUN_ID, 201L, 4L, "SUCCESS", null, null, null);

        assertTrue(resp.isSuccess());
        verify(stageMapper, never()).updateById(any(RunStage.class));
        verify(runMapper, never()).updateById(any(Run.class));
    }

    @Test
    void 配置验签密钥后签名不符_拒绝回调() throws Exception {
        setSecret("topsecret");

        R<String> resp = controller.status(RUN_ID, 201L, 4L, "SUCCESS", null, "badsign", null);

        assertFalse(resp.isSuccess());
        verifyNoInteractions(runMapper, stageMapper);
    }

    @Test
    void 构建前置失败回调_stageId0_运行置失败且全部未开始阶段跳过() {
        Run run = runningRun();
        when(runMapper.selectById(RUN_ID)).thenReturn(run);
        // Checkout 失败意味着构建未进入任何 Ferry 阶段，8 个阶段全部未开始
        List<RunStage> all = List.of(
                stage(201, 1, 1, StageStatusEnum.NOT_STARTED.getCode()),
                stage(202, 2, 2, StageStatusEnum.NOT_STARTED.getCode()),
                stage(203, 3, 3, StageStatusEnum.NOT_STARTED.getCode()),
                stage(204, 4, 4, StageStatusEnum.NOT_STARTED.getCode()),
                stage(205, 5, 5, StageStatusEnum.NOT_STARTED.getCode()),
                stage(206, 6, 6, StageStatusEnum.NOT_STARTED.getCode()),
                stage(207, 7, 7, StageStatusEnum.NOT_STARTED.getCode()),
                stage(208, 8, 8, StageStatusEnum.NOT_STARTED.getCode()));
        when(stageMapper.selectByRun(RUN_ID)).thenReturn(all);
        when(taskMapper.selectByStage(201L)).thenReturn(
                List.of(task(301, 201, StageStatusEnum.NOT_STARTED.getCode())));

        R<String> resp = controller.status(RUN_ID, 0L, 5L, "FAILURE", null, null, null);

        assertTrue(resp.isSuccess());
        ArgumentCaptor<Run> runCaptor = ArgumentCaptor.forClass(Run.class);
        verify(runMapper).updateById(runCaptor.capture());
        assertEquals(RunStatusEnum.FAILURE.getCode(), runCaptor.getValue().getStatus());
        assertNotNull(runCaptor.getValue().getEndTime());
        // 8 个未开始阶段（含 CI）全部置跳过
        ArgumentCaptor<RunStage> stageCaptor = ArgumentCaptor.forClass(RunStage.class);
        verify(stageMapper, times(8)).updateById(stageCaptor.capture());
        assertEquals(8, stageCaptor.getAllValues().stream()
                .filter(s -> StageStatusEnum.SKIPPED.getCode().equals(s.getStatus())).count());
        // 任务镜像：回调阶段对应任务同步置跳过
        ArgumentCaptor<RunTask> taskCaptor = ArgumentCaptor.forClass(RunTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertEquals(6, taskCaptor.getValue().getStatus());
    }

    @Test
    void 构建前置回调运行态或成功_无意义忽略() {
        when(runMapper.selectById(RUN_ID)).thenReturn(runningRun());

        assertTrue(controller.status(RUN_ID, 0L, 5L, "SUCCESS", null, null, null).isSuccess());
        assertTrue(controller.status(RUN_ID, 0L, 5L, "RUNNING", null, null, null).isSuccess());

        verify(runMapper, never()).updateById(any(Run.class));
        verify(stageMapper, never()).updateById(any(RunStage.class));
    }

    @Test
    void 构建前置失败回调_运行已终态_幂等忽略() {
        Run run = runningRun();
        run.setStatus(RunStatusEnum.FAILURE.getCode());
        when(runMapper.selectById(RUN_ID)).thenReturn(run);

        R<String> resp = controller.status(RUN_ID, 0L, 5L, "FAILURE", null, null, null);

        assertTrue(resp.isSuccess());
        verify(runMapper, never()).updateById(any(Run.class));
        verify(stageMapper, never()).updateById(any(RunStage.class));
    }
}
