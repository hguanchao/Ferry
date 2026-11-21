package com.ferry.devops.biz.integration.jenkins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Job 命名规则测试：Job 名 = 应用编码（应用级 Job），白名单校验防路径注入。
 */
class JenkinsJobNamesTest {

    @Test
    void 应用编码即Job名_自动去除首尾空白() {
        assertEquals("order-service", JenkinsJobNames.appJobName("order-service"));
        assertEquals("order-service", JenkinsJobNames.appJobName(" order-service "));
    }

    @Test
    void 编码含中划线与数字时仍合法() {
        assertEquals("svc-2", JenkinsJobNames.appJobName("svc-2"));
    }

    @Test
    void 空编码_含路径符或空格的编码均拒绝() {
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.appJobName(""));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.appJobName(null));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.appJobName("../evil"));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.appJobName("svc name"));
    }
}
