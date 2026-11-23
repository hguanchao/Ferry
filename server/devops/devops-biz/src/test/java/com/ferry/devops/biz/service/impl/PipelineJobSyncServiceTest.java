package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.model.JenkinsJob;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchException;
import com.ferry.devops.biz.integration.jenkins.JenkinsJobNames;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.service.IPipelineStageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Job 同步编排测试：状态机规则（成功/失败/停用例外/待配置）、active-run 保护、Job 名与禁用位下发。
 */
@ExtendWith(MockitoExtension.class)
class PipelineJobSyncServiceTest {

    private static final long PIPELINE_ID = 701L;

    @Mock
    private PipelineMapper pipelineMapper;
    @Mock
    private AppMapper appMapper;
    @Mock
    private IPipelineStageService pipelineStageService;
    @Mock
    private RunMapper runMapper;
    @Mock
    private JenkinsClient jenkinsClient;

    private PipelineJobSyncService service;

    @BeforeEach
    void setUp() {
        service = new PipelineJobSyncService(pipelineMapper, appMapper, pipelineStageService, runMapper, jenkinsClient);
    }

    private Pipeline pipeline(int version, PipelineStatusEnum status) {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(PIPELINE_ID);
        pipeline.setAppId(1L);
        pipeline.setName("主干流水线");
        pipeline.setVersion(version);
        pipeline.setStatus(status.getCode());
        return pipeline;
    }

    private App app() {
        App app = new App();
        app.setId(1L);
        app.setCode("order-service");
        app.setName("订单服务");
        app.setRepoUrl("https://github.com/yangzongzhuan/RuoYi");
        return app;
    }

    private PipelineDefinitionDTO definitionWithCiStage() {
        PipelineStageTaskDTO task = new PipelineStageTaskDTO();
        task.setName("打包");
        task.setExecutor(1);
        task.setScriptContent("mvn -B package");
        PipelineStageDTO stage = new PipelineStageDTO();
        stage.setType(1);
        stage.setName("构建");
        stage.setTasks(List.of(task));
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        definition.setStages(List.of(stage));
        return definition;
    }

    private PipelineDefinitionDTO emptyDefinition() {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        definition.setStages(List.of());
        return definition;
    }

    /** 捕获 createJob 下发的 Job 配置 */
    private JenkinsJob captureCreatedJob() {
        ArgumentCaptor<JenkinsJob> captor = ArgumentCaptor.forClass(JenkinsJob.class);
        verify(jenkinsClient).createJob(captor.capture());
        return captor.getValue();
    }

    /** 捕获 updateJob 下发的 Job 配置 */
    private JenkinsJob captureUpdatedJob() {
        ArgumentCaptor<JenkinsJob> captor = ArgumentCaptor.forClass(JenkinsJob.class);
        verify(jenkinsClient).updateJob(captor.capture());
        return captor.getValue();
    }

    @Test
    void 启用态同步成功_状态保持启用并下发正确Job名() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(1, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 1)).thenReturn(definitionWithCiStage());
        when(runMapper.countActiveByPipeline(PIPELINE_ID)).thenReturn(0L);

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertTrue(outcome.synced());
        assertEquals(PipelineStatusEnum.ENABLED.getCode(), outcome.status());
        // Job 名 = 应用编码；GitHub 项目属性随应用档案下发
        JenkinsJob created = captureCreatedJob();
        assertEquals(JenkinsJobNames.appJobName("order-service"), created.getName());
        assertEquals("order-service", created.getAppCode());
        assertFalse(created.isDisabled());
        assertEquals("https://github.com/yangzongzhuan/RuoYi", created.getRepoUrl());
        assertEquals("订单服务", created.getRepoName());
        JenkinsJob updated = captureUpdatedJob();
        assertEquals(JenkinsJobNames.appJobName("order-service"), updated.getName());
        assertFalse(updated.isDisabled());
        ArgumentCaptor<Pipeline> captor = ArgumentCaptor.forClass(Pipeline.class);
        verify(pipelineMapper).updateById(captor.capture());
        assertEquals(PipelineStatusEnum.ENABLED.getCode(), captor.getValue().getStatus());
    }

    @Test
    void 空定义同步成功_落待配置且Job禁用() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(0, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 0)).thenReturn(emptyDefinition());

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertTrue(outcome.synced());
        assertEquals(PipelineStatusEnum.PENDING_CONFIG.getCode(), outcome.status());
        JenkinsJob created = captureCreatedJob();
        assertEquals("order-service", created.getAppCode());
        assertTrue(created.isDisabled());
        assertTrue(captureUpdatedJob().isDisabled());
    }

    @Test
    void 停用流水线同步成功_保持停用且Job禁用() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(2, PipelineStatusEnum.DISABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 2)).thenReturn(definitionWithCiStage());
        when(runMapper.countActiveByPipeline(PIPELINE_ID)).thenReturn(0L);

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertTrue(outcome.synced());
        assertEquals(PipelineStatusEnum.DISABLED.getCode(), outcome.status());
        assertTrue(captureCreatedJob().isDisabled());
        assertTrue(captureUpdatedJob().isDisabled());
    }

    @Test
    void 存在进行中构建时拒绝覆盖脚本_落同步失败且不下发Jenkins() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(2, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 2)).thenReturn(definitionWithCiStage());
        when(runMapper.countActiveByPipeline(PIPELINE_ID)).thenReturn(1L);

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertFalse(outcome.synced());
        assertEquals(PipelineStatusEnum.SYNC_FAILED.getCode(), outcome.status());
        assertTrue(outcome.message().contains("暂不能覆盖可执行脚本"));
        assertTrue(outcome.message().contains("重试同步"));
        verify(jenkinsClient, never()).createJob(any(JenkinsJob.class));
        verify(jenkinsClient, never()).updateJob(any(JenkinsJob.class));
    }

    @Test
    void 空定义不受activeRun保护限制_允许清理为禁用空Job() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(2, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 2)).thenReturn(emptyDefinition());

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertTrue(outcome.synced());
        assertTrue(captureCreatedJob().isDisabled());
        assertTrue(captureUpdatedJob().isDisabled());
    }

    @Test
    void Jenkins明确失败_落同步失败并回传原因() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(1, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 1)).thenReturn(definitionWithCiStage());
        when(runMapper.countActiveByPipeline(PIPELINE_ID)).thenReturn(0L);
        doThrow(new JenkinsDispatchException("更新 Jenkins Job 失败：HTTP 403", false))
                .when(jenkinsClient).updateJob(any(JenkinsJob.class));

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertFalse(outcome.synced());
        assertEquals(PipelineStatusEnum.SYNC_FAILED.getCode(), outcome.status());
        assertTrue(outcome.message().contains("403"));
    }

    @Test
    void Jenkins不确定失败_文案脱敏() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(1, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 1)).thenReturn(definitionWithCiStage());
        when(runMapper.countActiveByPipeline(PIPELINE_ID)).thenReturn(0L);
        doThrow(new JenkinsDispatchException("Jenkins 请求失败：/job/secret-host/x", true))
                .when(jenkinsClient).updateJob(any(JenkinsJob.class));

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertFalse(outcome.synced());
        // 不确定类失败不透传原始路径，避免向页面泄露内部信息
        assertEquals("Jenkins 连接异常，结果待确认", outcome.message());
    }

    @Test
    void 停用流水线同步失败_保持停用安全态() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(2, PipelineStatusEnum.DISABLED));
        when(appMapper.selectById(1L)).thenReturn(app());
        when(pipelineStageService.loadDefinition(PIPELINE_ID, 2)).thenReturn(definitionWithCiStage());
        when(runMapper.countActiveByPipeline(PIPELINE_ID)).thenReturn(0L);
        doThrow(new JenkinsDispatchException("更新失败", false))
                .when(jenkinsClient).updateJob(any(JenkinsJob.class));

        PipelineJobSyncService.SyncOutcome outcome = service.sync(PIPELINE_ID);

        assertFalse(outcome.synced());
        assertEquals(PipelineStatusEnum.DISABLED.getCode(), outcome.status());
    }

    @Test
    void 应用缺失或编码为空_抛业务异常且不下发() {
        when(pipelineMapper.selectById(PIPELINE_ID)).thenReturn(pipeline(1, PipelineStatusEnum.ENABLED));
        when(appMapper.selectById(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.sync(PIPELINE_ID));
        verify(jenkinsClient, never()).createJob(any(JenkinsJob.class));
        verify(jenkinsClient, never()).updateJob(any(JenkinsJob.class));
    }
}
