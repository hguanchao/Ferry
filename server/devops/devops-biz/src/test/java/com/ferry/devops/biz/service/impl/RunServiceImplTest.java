package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.devops.api.entity.RunTrigger;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.RunLogSegmentMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunOperationMapper;
import com.ferry.devops.biz.mapper.RunPrecheckMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.ferry.devops.biz.mapper.RunTriggerMapper;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineStageService;
import com.ferry.devops.biz.service.IRunStageService;
import com.ferry.devops.api.vo.TriggerRecordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 运行服务测试：触发记录分页查询。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunServiceImplTest {

    private static final Long APP_ID = 10L;

    @Mock
    private RunMapper runMapper;
    @Mock
    private PipelineMapper pipelineMapper;
    @Mock
    private IPipelineStageService pipelineStageService;
    @Mock
    private RunStageMapper stageMapper;
    @Mock
    private RunStageTaskMapper taskMapper;
    @Mock
    private RunLogSegmentMapper logMapper;
    @Mock
    private RunPrecheckMapper precheckMapper;
    @Mock
    private RunTriggerMapper triggerMapper;
    @Mock
    private RunOperationMapper operationMapper;
    @Mock
    private JenkinsClient jenkinsClient;
    @Mock
    private AppMapper appMapper;
    @Mock
    private IPipelineService pipelineService;
    @Mock
    private IRunStageService runStageService;

    private RunServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RunServiceImpl(runMapper, pipelineMapper, pipelineStageService, stageMapper,
                taskMapper, logMapper, precheckMapper, triggerMapper, operationMapper, jenkinsClient,
                appMapper, pipelineService, runStageService);
    }

    @Test
    void 触发记录_按应用查询并分页() {
        when(triggerMapper.selectRecentByApp(APP_ID)).thenReturn(List.of(triggerRecord(1L), triggerRecord(2L)));

        IPage<TriggerRecordVO> page = service.triggerRecords(APP_ID, 2L, 1L);

        verify(triggerMapper).selectRecentByApp(APP_ID);
        assertEquals(2, page.getTotal());
        assertEquals(2, page.getCurrent());
        assertEquals(1, page.getSize());
        // triggerRecords 只设置分页元信息，records 为全量（分页由查询侧保证）
        assertEquals(2, page.getRecords().size());
    }

    @Test
    void 触发记录_分页参数缺省用默认值() {
        when(triggerMapper.selectRecentByApp(APP_ID)).thenReturn(List.of());

        IPage<TriggerRecordVO> page = service.triggerRecords(APP_ID, null, null);

        assertEquals(1, page.getCurrent());
        assertEquals(20, page.getSize());
        assertEquals(0, page.getTotal());
    }

    private RunTrigger triggerRecord(Long id) {
        RunTrigger record = new RunTrigger();
        record.setId(id);
        record.setAppId(APP_ID);
        record.setPipelineId(20L);
        return record;
    }
}
