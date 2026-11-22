package com.ferry.devops.biz.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.RunStageGate;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import com.ferry.devops.biz.gate.GateReportContract;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.service.IRunStageGateService;
import com.ferry.devops.biz.service.IRunStageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阶段门禁服务测试：归一化报告解析、通过/不通过/数据获取失败与阶段收敛。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunStageGateEvaluatorTest {

    private static final Long RUN_ID = 100L;
    private static final Long STAGE_ID = 200L;
    private static final Long APP_ID = 1L;

    @Mock
    private RunMapper runMapper;
    @Mock
    private RunStageMapper stageMapper;
    @Mock
    private AppMapper appMapper;
    @Mock
    private RunStageGateMapper gateResultMapper;
    @Mock
    private IRunStageGateService runStageGateService;
    @Mock
    private IRunStageService runStageService;
    @Mock
    private JenkinsClient jenkinsClient;

    private DevopsIntegrationProperties properties;
    private RunStageGateEvaluator service;

    @BeforeEach
    void setUp() {
        properties = new DevopsIntegrationProperties();
        properties.getGate().setEnabled(true);
        properties.getGate().setPullTimeoutSeconds(300);
        service = new RunStageGateEvaluator(runMapper, stageMapper, appMapper, gateResultMapper,
                runStageGateService, runStageService, jenkinsClient, properties);
    }

    private Run stageRun() {
        Run run = new Run();
        run.setId(RUN_ID);
        run.setAppId(APP_ID);
        run.setBranch("main");
        run.setJenkinsJobId("job");
        run.setJenkinsBuildNo(1L);
        return run;
    }

    private RunStage stage(int type) {
        RunStage stage = new RunStage();
        stage.setId(STAGE_ID);
        stage.setRunId(RUN_ID);
        stage.setType(type);
        stage.setStatus(StageStatusEnum.PENDING_GATE.getCode());
        stage.setEndTime(new Date());
        return stage;
    }

    @Test
    void 单测报告通过_按通过率判定并阶段成功() {
        RunStage stage = stage(2);
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(stageRun());
        when(appMapper.selectById(APP_ID)).thenReturn(new App());
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 2, 1)).thenReturn(true);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 2, 2)).thenReturn(false);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 2, 3)).thenReturn(false);
        when(jenkinsClient.fetchArtifact("job", 1L, GateReportContract.UNIT_TEST_REPORT_PATH))
                .thenReturn(Optional.of("""
                        {"schemaVersion":"1.0","tests":100,"failures":2,"errors":1,
                         "lineCovered":800,"lineMissed":200,"branchCovered":40,"branchMissed":10}
                        """));
        when(gateResultMapper.listByStage(STAGE_ID)).thenReturn(List.of());
        when(gateResultMapper.countBlockingByStage(STAGE_ID)).thenReturn(0L);

        service.evaluate(STAGE_ID);

        verify(runStageGateService).judge(eq(RUN_ID), eq(STAGE_ID), eq(APP_ID), eq("main"), eq(2),
                eq(1), eq("97.00"), eq(GateReportContract.UNIT_TEST_REPORT_PATH), eq("1.0"));
        verify(runStageService).transitionStage(eq(STAGE_ID), eq(StageStatusEnum.SUCCESS), any(Date.class));
    }

    @Test
    void 单测通过率分母为0_判数据获取失败并阶段阻塞() {
        RunStage stage = stage(2);
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(stageRun());
        when(appMapper.selectById(APP_ID)).thenReturn(new App());
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 2, 1)).thenReturn(true);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 2, 2)).thenReturn(false);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 2, 3)).thenReturn(false);
        when(jenkinsClient.fetchArtifact("job", 1L, GateReportContract.UNIT_TEST_REPORT_PATH))
                .thenReturn(Optional.of("""
                        {"schemaVersion":"1.0","tests":0,"failures":0,"errors":0,
                         "lineCovered":0,"lineMissed":0,"branchCovered":0,"branchMissed":0}
                        """));
        when(gateResultMapper.listByStage(STAGE_ID)).thenReturn(List.of());
        when(gateResultMapper.countBlockingByStage(STAGE_ID)).thenReturn(1L);

        service.evaluate(STAGE_ID);

        verify(runStageGateService).judgeFetchFailed(eq(RUN_ID), eq(STAGE_ID), eq(APP_ID), eq("main"),
                eq(2), eq(1), eq(GateReportContract.UNIT_TEST_REPORT_PATH), eq("1.0"), anyString());
        verify(runStageService).transitionStage(eq(STAGE_ID), eq(StageStatusEnum.BLOCKED), any(Date.class));
    }

    @Test
    void 扫描报告不通过_阶段阻塞() {
        RunStage stage = stage(3);
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(stageRun());
        when(appMapper.selectById(APP_ID)).thenReturn(new App());
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 3, 4)).thenReturn(true);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 3, 5)).thenReturn(false);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 3, 6)).thenReturn(false);
        when(jenkinsClient.fetchArtifact("job", 1L, GateReportContract.SCAN_REPORT_PATH))
                .thenReturn(Optional.of("""
                        {"schemaVersion":"1.0","values":{"4":1,"5":0,"6":"MIT"}}
                        """));
        when(gateResultMapper.listByStage(STAGE_ID)).thenReturn(List.of());
        when(gateResultMapper.countBlockingByStage(STAGE_ID)).thenReturn(1L);

        service.evaluate(STAGE_ID);

        verify(runStageGateService).judge(eq(RUN_ID), eq(STAGE_ID), eq(APP_ID), eq("main"), eq(3),
                eq(4), eq("1"), eq(GateReportContract.SCAN_REPORT_PATH), eq("1.0"));
        verify(runStageService).transitionStage(eq(STAGE_ID), eq(StageStatusEnum.BLOCKED), any(Date.class));
    }

    @Test
    void 报告缺失且超时_判数据获取失败并阶段阻塞() {
        RunStage stage = stage(3);
        stage.setEndTime(new Date(System.currentTimeMillis() - 600_000L));
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(stageRun());
        when(appMapper.selectById(APP_ID)).thenReturn(new App());
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 3, 4)).thenReturn(true);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 3, 5)).thenReturn(false);
        when(runStageGateService.hasApplicableRule(APP_ID, "main", 3, 6)).thenReturn(false);
        when(jenkinsClient.fetchArtifact("job", 1L, GateReportContract.SCAN_REPORT_PATH))
                .thenReturn(Optional.empty());
        when(gateResultMapper.listByStage(STAGE_ID)).thenReturn(List.of());
        when(gateResultMapper.countBlockingByStage(STAGE_ID)).thenReturn(1L);

        service.evaluate(STAGE_ID);

        verify(runStageGateService).judgeFetchFailed(eq(RUN_ID), eq(STAGE_ID), eq(APP_ID), eq("main"),
                eq(3), eq(4), eq(GateReportContract.SCAN_REPORT_PATH), eq("1.0"), anyString());
        verify(runStageService).transitionStage(eq(STAGE_ID), eq(StageStatusEnum.BLOCKED), any(Date.class));
    }

    @Test
    void 无适用规则_不拉报告直接成功() {
        RunStage stage = stage(3);
        when(stageMapper.selectById(STAGE_ID)).thenReturn(stage);
        when(runMapper.selectById(RUN_ID)).thenReturn(stageRun());
        when(runStageGateService.hasApplicableRule(anyLong(), anyString(), eq(3), anyInt())).thenReturn(false);
        when(gateResultMapper.listByStage(STAGE_ID)).thenReturn(List.of());

        service.evaluate(STAGE_ID);

        verify(jenkinsClient, never()).fetchArtifact(anyString(), anyLong(), anyString());
        verify(runStageService).transitionStage(eq(STAGE_ID), eq(StageStatusEnum.SUCCESS), any(Date.class));
    }
}
