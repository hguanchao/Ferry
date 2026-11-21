package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.mapper.AppMapper;
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

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 应用服务测试：创建/编辑应用联动初始化与刷新 Jenkins 裸 Job（Job 名 = 应用编码），
 * 删除应用联动清理 Job，Jenkins 失败不影响应用数据落库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppServiceImplTest {

    private static final long APP_ID = 1L;

    @Mock
    private AppMapper appMapper;
    @Mock
    private JenkinsClient jenkinsClient;

    private AppServiceImpl service;

    @BeforeAll
    static void initTableMeta() {
        // 无 MyBatis 运行时，手动注册实体表信息以支持 saveOrUpdate 的主键判断
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), App.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new AppServiceImpl(jenkinsClient);
        injectBaseMapper();
    }

    /** MP ServiceImpl 的 baseMapper 为注入字段，单测环境用反射塞入 Mock */
    private void injectBaseMapper() throws Exception {
        Class<?> type = service.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if ("baseMapper".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(service, appMapper);
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("未找到 baseMapper 字段");
    }

    private AppDTO createBody() {
        AppDTO body = new AppDTO();
        body.setCode("order-service");
        body.setName("订单服务");
        body.setRepoUrl("https://github.com/yangzongzhuan/RuoYi");
        return body;
    }

    @Test
    void 新建应用成功后按应用编码初始化禁用态空白Job() {
        when(appMapper.selectCount(any())).thenReturn(0L);
        when(appMapper.insertOrUpdate(any(App.class))).thenReturn(true);

        assertTrue(service.save(createBody()));

        verify(jenkinsClient).createJob(eq("order-service"), eq("order-service"), anyString(), eq(true),
                eq("https://github.com/yangzongzhuan/RuoYi"), eq("订单服务"));
        // 新建分支只走 createJob 初始化空白 Job
        verify(jenkinsClient, never()).updateJob(anyString(), anyString(), anyString(), anyBoolean(), any(), any());
    }

    @Test
    void 编辑应用修改仓库地址后Job的GitHub属性跟随刷新() {
        App old = new App();
        old.setId(APP_ID);
        old.setCode("order-service");
        old.setName("订单服务");
        old.setRepoUrl("https://github.com/yangzongzhuan/RuoYi");
        old.setRepoToken("old-token");
        when(appMapper.selectById(APP_ID)).thenReturn(old);
        when(appMapper.selectCount(any())).thenReturn(0L);
        when(appMapper.insertOrUpdate(any(App.class))).thenReturn(true);
        AppDTO body = createBody();
        body.setId(APP_ID);
        body.setRepoUrl("https://gitee.com/y_project/RuoYi");
        // repoToken 传空保留旧令牌
        body.setRepoToken("");

        assertTrue(service.save(body));

        // MP 3.5.18 saveOrUpdate 统一走 insertOrUpdate，实体上携带保留后的令牌
        ArgumentCaptor<App> appCaptor = ArgumentCaptor.forClass(App.class);
        verify(appMapper).insertOrUpdate(appCaptor.capture());
        assertEquals("old-token", appCaptor.getValue().getRepoToken());
        // 编辑走 updateJob 刷新脚本与 GitHub 项目属性，不再重复创建
        verify(jenkinsClient).updateJob(eq("order-service"), eq("order-service"), anyString(), eq(true),
                eq("https://gitee.com/y_project/RuoYi"), eq("订单服务"));
        verify(jenkinsClient, never()).createJob(anyString(), anyString(), anyString(), anyBoolean(), any(), any());
    }

    @Test
    void 删除应用后同步删除对应JenkinsJob() {
        App app = new App();
        app.setId(APP_ID);
        app.setCode("order-service");
        when(appMapper.existsUnfinishedBusiness(List.of(APP_ID))).thenReturn(false);
        when(appMapper.selectByIds(List.of(APP_ID))).thenReturn(List.of(app));
        when(appMapper.deleteByIds(List.of(APP_ID))).thenReturn(1);

        assertTrue(service.deleted(List.of(APP_ID)));

        verify(jenkinsClient).deleteJob("order-service");
    }

    @Test
    void 应用存在进行中运行或部署或未关闭发布单时拒绝删除() {
        when(appMapper.existsUnfinishedBusiness(List.of(APP_ID))).thenReturn(true);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleted(List.of(APP_ID)));

        assertTrue(exception.getMessage().contains("不可删除"));
        verify(jenkinsClient, never()).deleteJob(anyString());
    }

    @Test
    void Jenkins创建失败_应用创建一起回滚() {
        when(appMapper.selectCount(any())).thenReturn(0L);
        when(appMapper.insertOrUpdate(any(App.class))).thenReturn(true);
        doThrow(new RuntimeException("jenkins down")).when(jenkinsClient)
                .createJob(anyString(), anyString(), anyString(), anyBoolean(), any(), any());

        // Job 与应用同生命周期：联动失败异常上抛，事务整体回滚，应用不落库
        assertThrows(RuntimeException.class, () -> service.save(createBody()));

        verify(jenkinsClient).createJob(anyString(), anyString(), anyString(), eq(true), any(), any());
        verify(jenkinsClient, never()).updateJob(anyString(), anyString(), anyString(), anyBoolean(), any(), any());
    }

    @Test
    void Jenkins删除失败_应用删除一起回滚() {
        App app = new App();
        app.setId(APP_ID);
        app.setCode("order-service");
        when(appMapper.existsUnfinishedBusiness(List.of(APP_ID))).thenReturn(false);
        when(appMapper.selectByIds(List.of(APP_ID))).thenReturn(List.of(app));
        when(appMapper.deleteByIds(List.of(APP_ID))).thenReturn(1);
        doThrow(new RuntimeException("jenkins down")).when(jenkinsClient).deleteJob(anyString());

        assertThrows(RuntimeException.class, () -> service.deleted(List.of(APP_ID)));

        verify(jenkinsClient).deleteJob("order-service");
    }

    @Test
    void 未登记仓库地址的应用_初始化Job不携带GitHub属性() {
        when(appMapper.selectCount(any())).thenReturn(0L);
        when(appMapper.insertOrUpdate(any(App.class))).thenReturn(true);
        AppDTO body = createBody();
        body.setRepoUrl(null);

        assertTrue(service.save(body));

        verify(jenkinsClient).createJob(eq("order-service"), eq("order-service"), anyString(), eq(true),
                isNull(), eq("订单服务"));
        verify(jenkinsClient, never()).updateJob(anyString(), anyString(), anyString(), anyBoolean(), any(), any());
        // 空 url 下不生成 GitHub 项目属性（由 XML 层保证），这里只验证传参透传
        verify(jenkinsClient, never()).deleteJob(anyString());
    }
}
