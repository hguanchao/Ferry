package com.ferry.devops.biz.integration.jenkins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Job 命名规则测试：{@code <应用编码>-pipeline-<流水线id>}，白名单校验防路径注入。
 */
class JenkinsJobNamesTest {

    @Test
    void 正常编码与流水线id拼接为规范Job名() {
        assertEquals("order-service-pipeline-701", JenkinsJobNames.pipelineJobName("order-service", 701L));
        assertEquals("order-service-pipeline-701", JenkinsJobNames.pipelineJobName(" order-service ", 701L));
    }

    @Test
    void 编码后缀含中划线与数字时仍合法() {
        assertEquals("svc-2-pipeline-3", JenkinsJobNames.pipelineJobName("svc-2", 3L));
    }

    @Test
    void 空编码_空id_含路径符的编码均拒绝() {
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.pipelineJobName("", 1L));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.pipelineJobName(null, 1L));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.pipelineJobName("svc", null));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.pipelineJobName("../evil", 1L));
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobNames.pipelineJobName("svc name", 1L));
    }
}
