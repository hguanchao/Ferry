package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IPipelineStageService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流水线服务测试：保存编排（版本推进/同步/空定义）、定义响应与状态更新。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineServiceImplTest {

    private static final long PIPELINE_ID = 701L;

    @Mock
    private PipelineMapper pipelineMapper;
    @Mock
    private IPipelineStageService pipelineStageService;
    @Mock
    private PipelineJobSyncService jobSyncService;
    @Mock
    private IAppService appService;

    private PipelineServiceImpl service;

    @BeforeAll
    static void initTableMeta() {
        // 无 MyBatis 运行时，手动注册实体表信息以支持 saveOrUpdate 的主键判断
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Pipeline.class);
        // JsonUtil 的 ObjectMapper 由 Spring 容器构造注入，单测环境手动初始化静态字段
        new JsonUtil(new ObjectMapper());
    }

    @BeforeEach
    void setUp() throws Exception {
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        lenient().when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new PipelineServiceImpl(appService, jobSyncService, pipelineStageService,
                new TransactionTemplate(txManager));
        injectBaseMapper();
    }

    /** MP ServiceImpl 的 baseMapper 为注入字段，单测环境用反射塞入 Mock */
    private void injectBaseMapper() throws Exception {
        Class<?> type = service.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if ("baseMapper".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(service, pipelineMapper);
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("未找到 baseMapper 字段");
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

    private PipelineStageDTO ciStage(String taskName) {
        PipelineStageTaskDTO task = new PipelineStageTaskDTO();
        task.setName(taskName);
        task.setExecutor(1);
        task.setScriptContent("mvn -B package");
        PipelineStageDTO stage = new PipelineStageDTO();
        stage.setType(1);
        stage.setName("构建");
        stage.setTasks(List.of(task));
        return stage;
    }

    private PipelineDefinitionDTO ciDefinition(String taskName) {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        definition.setStages(List.of(ciStage(taskName)));
        return definition;
    }

    @Test
    void 新建携带阶段定义_落库V0并同步() {
        PipelineDTO body = new PipelineDTO();
        body.setAppId(1L);
        body.setName("主干流水线");
        body.setStages(List.of(ciStage("打包")));
        // MP 3.5.18 的 saveOrUpdate 走 BaseMapper.insertOrUpdate，用 Answer 模拟主键回填
        lenient().when(pipelineMapper.insertOrUpdate(any(Pipeline.class))).thenAnswer(inv -> {
            inv.getArgument(0, Pipeline.class).setId(PIPELINE_ID);
            return true;
        });

        assertTrue(service.save(body));

        ArgumentCaptor<List<PipelineStageDTO>> stagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(pipelineStageService).save(stagesCaptor.capture());
        PipelineStageDTO savedStage = stagesCaptor.getValue().get(0);
        assertEquals(PIPELINE_ID, savedStage.getPipelineId().longValue());
        assertEquals(0, savedStage.getVersion());
        assertEquals(1, savedStage.getSort());
        verify(jobSyncService).sync(PIPELINE_ID);
    }

    @Test
    void 新建未提交阶段_不落定义但仍同步() {
        PipelineDTO body = new PipelineDTO();
        body.setAppId(1L);
        body.setName("主干流水线");
        lenient().when(pipelineMapper.insertOrUpdate(any(Pipeline.class))).thenAnswer(inv -> {
            inv.getArgument(0, Pipeline.class).setId(PIPELINE_ID);
            return true;
        });

        assertTrue(service.save(body));

        verify(pipelineStageService, never()).save(anyList());
        verify(jobSyncService).sync(PIPELINE_ID);
    }

    @Test
    void 编辑_版本递增并保存阶段() {
        PipelineDTO body = new PipelineDTO();
        body.setId(PIPELINE_ID);
        body.setAppId(1L);
        body.setName("主干流水线");
        body.setStages(List.of(ciStage("打包")));
        lenient().when(pipelineMapper.selectById(PIPELINE_ID)).thenAnswer(inv -> pipeline(2, PipelineStatusEnum.ENABLED));
        lenient().when(pipelineMapper.insertOrUpdate(any(Pipeline.class))).thenReturn(true);

        assertTrue(service.save(body));

        ArgumentCaptor<Pipeline> pipelineCaptor = ArgumentCaptor.forClass(Pipeline.class);
        verify(pipelineMapper).insertOrUpdate(pipelineCaptor.capture());
        assertEquals(3, pipelineCaptor.getValue().getVersion());
        ArgumentCaptor<List<PipelineStageDTO>> stagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(pipelineStageService).save(stagesCaptor.capture());
        assertEquals(3, stagesCaptor.getValue().get(0).getVersion());
        verify(jobSyncService).sync(PIPELINE_ID);
    }

    @Test
    void 编辑提交空阶段_仍同步以禁用Job() {
        PipelineDTO body = new PipelineDTO();
        body.setId(PIPELINE_ID);
        body.setAppId(1L);
        body.setName("主干流水线");
        body.setStages(List.of());
        lenient().when(pipelineMapper.selectById(PIPELINE_ID)).thenAnswer(inv -> pipeline(2, PipelineStatusEnum.ENABLED));
        lenient().when(pipelineMapper.insertOrUpdate(any(Pipeline.class))).thenReturn(true);

        assertTrue(service.save(body));

        ArgumentCaptor<Pipeline> pipelineCaptor = ArgumentCaptor.forClass(Pipeline.class);
        verify(pipelineMapper).insertOrUpdate(pipelineCaptor.capture());
        assertEquals(3, pipelineCaptor.getValue().getVersion());
        verify(pipelineStageService, never()).save(anyList());
        verify(jobSyncService).sync(PIPELINE_ID);
    }

    @Test
    void 编辑未提交阶段_按清空处理并同步() {
        PipelineDTO body = new PipelineDTO();
        body.setId(PIPELINE_ID);
        body.setAppId(1L);
        body.setName("主干流水线");
        lenient().when(pipelineMapper.selectById(PIPELINE_ID)).thenAnswer(inv -> pipeline(2, PipelineStatusEnum.ENABLED));
        lenient().when(pipelineMapper.insertOrUpdate(any(Pipeline.class))).thenReturn(true);

        assertTrue(service.save(body));

        ArgumentCaptor<Pipeline> pipelineCaptor = ArgumentCaptor.forClass(Pipeline.class);
        verify(pipelineMapper).insertOrUpdate(pipelineCaptor.capture());
        assertEquals(3, pipelineCaptor.getValue().getVersion());
        verify(pipelineStageService, never()).save(anyList());
        verify(jobSyncService).sync(PIPELINE_ID);
    }

    @Test
    void 批量删除_存在进行中运行_拒绝且不删除() {
        when(pipelineMapper.countActiveRun(List.of(1L, 2L))).thenReturn(1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleted(List.of(1L, 2L)));

        assertTrue(exception.getMessage().contains("进行中的运行"));
        verify(pipelineStageService, never()).deleted(anyList());
    }

    @Test
    void 批量删除_无进行中运行_删除全部() {
        when(pipelineMapper.countActiveRun(List.of(1L, 2L))).thenReturn(0L);
        when(pipelineMapper.deleteByIds(List.of(1L, 2L))).thenReturn(2);

        assertTrue(service.deleted(List.of(1L, 2L)));

        verify(pipelineMapper).countActiveRun(List.of(1L, 2L));
        verify(pipelineStageService).deleted(List.of(1L, 2L));
    }

    @Test
    void 启用流水线_更新成功返回true() {
        lenient().when(pipelineMapper.update(isNull(), any())).thenReturn(1);

        assertTrue(service.changeStatus(PIPELINE_ID, PipelineStatusEnum.ENABLED.getCode()));
        verify(jobSyncService, never()).sync(anyLong());
    }

    @Test
    void 变更状态未命中记录_返回false() {
        assertFalse(service.changeStatus(PIPELINE_ID, PipelineStatusEnum.ENABLED.getCode()));
    }

    @Test
    void 停用流水线_更新成功返回true() {
        lenient().when(pipelineMapper.update(isNull(), any())).thenReturn(1);

        assertTrue(service.changeStatus(PIPELINE_ID, PipelineStatusEnum.DISABLED.getCode()));
        verify(jobSyncService, never()).sync(anyLong());
    }

    @Test
    void 不支持的目标状态_拒绝且不读取流水线() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.changeStatus(PIPELINE_ID, PipelineStatusEnum.SYNC_FAILED.getCode()));

        assertTrue(exception.getMessage().contains("仅支持启用或停用"));
        verify(pipelineMapper, never()).selectById(anyLong());
        verify(jobSyncService, never()).sync(anyLong());
    }

    @Test
    void 同步重试入口_返回失败原因() {
        lenient().when(pipelineMapper.selectById(PIPELINE_ID)).thenAnswer(inv -> pipeline(1, PipelineStatusEnum.SYNC_FAILED));
        lenient().when(jobSyncService.sync(PIPELINE_ID))
                .thenReturn(new PipelineJobSyncService.SyncOutcome(false,
                        PipelineStatusEnum.SYNC_FAILED.getCode(), "更新 Jenkins Job 失败：HTTP 503"));

        assertEquals("同步失败：更新 Jenkins Job 失败：HTTP 503", service.sync(PIPELINE_ID));
    }

    @Test
    void 定义保存内容未变更且已同步_不建版本不重复同步() {
        lenient().when(pipelineMapper.selectById(PIPELINE_ID)).thenAnswer(inv -> pipeline(2, PipelineStatusEnum.ENABLED));
        lenient().when(pipelineStageService.loadDefinition(PIPELINE_ID, 2)).thenReturn(ciDefinition("打包"));

        VersionVO vo = service.saveDefinition(PIPELINE_ID, JsonUtil.toJson(ciDefinition("打包")), 1, "画布编辑");

        verify(pipelineStageService, never()).saveDefinition(anyLong(), anyInt(), any());
        verify(jobSyncService, never()).sync(anyLong());
        assertEquals(2, vo.getVersionNo());
        assertEquals(PipelineStatusEnum.ENABLED.getCode(), vo.getStatus());
    }

    @Test
    void 回退到旧版本_复制为新当前版本并同步() {
        // 版本号用状态桩模拟 CAS 递增，保证 versionVo 回读读到新版本
        AtomicInteger version = new AtomicInteger(3);
        lenient().when(pipelineMapper.selectById(PIPELINE_ID))
                .thenAnswer(inv -> pipeline(version.get(), PipelineStatusEnum.ENABLED));
        lenient().when(pipelineStageService.listVersions(PIPELINE_ID)).thenReturn(List.of(1, 2, 3));
        lenient().when(pipelineStageService.loadDefinitionJson(PIPELINE_ID, 1))
                .thenReturn(JsonUtil.toJson(ciDefinition("历史版本任务")));
        lenient().when(pipelineStageService.loadDefinition(PIPELINE_ID, 3)).thenReturn(ciDefinition("当前版本任务"));
        lenient().when(pipelineMapper.update(isNull(), any())).thenAnswer(inv -> {
            version.incrementAndGet();
            return 1;
        });
        lenient().when(jobSyncService.sync(PIPELINE_ID))
                .thenReturn(new PipelineJobSyncService.SyncOutcome(true, PipelineStatusEnum.ENABLED.getCode(), null));

        VersionVO vo = service.rollback(PIPELINE_ID, 1L, "回退到 V1");

        assertEquals(4, vo.getVersionNo());
        assertEquals(PipelineStatusEnum.ENABLED.getCode(), vo.getStatus());
        verify(pipelineStageService).saveDefinition(eq(PIPELINE_ID), eq(4),
                argThat(definition -> definition.getStages().get(0).getTasks().get(0).getName().equals("历史版本任务")));
    }

    @Test
    void 回退不存在的版本_拒绝() {
        lenient().when(pipelineMapper.selectById(PIPELINE_ID)).thenAnswer(inv -> pipeline(3, PipelineStatusEnum.ENABLED));
        lenient().when(pipelineStageService.listVersions(PIPELINE_ID)).thenReturn(List.of(1, 2, 3));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.rollback(PIPELINE_ID, 99L, "回退"));

        assertTrue(exception.getMessage().contains("回退版本不存在"));
    }
}
