package com.ferry.devops.biz.integration.jenkins;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jenkins Job 配置 XML 测试：最小配置生成、托管字段更新保留无关配置、归属校验与 XXE 加固。
 */
class JenkinsJobXmlTest {

    private static final String JOB = "order-service-pipeline-701";
    private static final String SCRIPT_V1 = "pipeline {\n    agent any\n}";

    @Test
    void 最小配置包含归属标记_九个参数_禁用位_且不含STAGE_SPEC() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, true);
        assertTrue(xml.contains("Managed by Ferry: " + JOB));
        assertTrue(xml.contains("order-service"));
        assertTrue(xml.contains("<disabled>true</disabled>"));
        assertEquals(9, xml.split("<hudson.model.StringParameterDefinition>", -1).length - 1);
        assertTrue(xml.contains("GIT_CREDENTIALS_ID"));
        assertFalse(xml.contains("STAGE_SPEC"));
        // 生成的配置可被安全解析并验证归属
        Document doc = JenkinsJobXml.loadManagedConfig(xml, JOB);
        assertNotNull(doc);
    }

    @Test
    void 脚本特殊字符经DOM转义_更新往返不丢失() {
        String script = "sh '''echo 'a' && ls <tmp> > out'''";
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", script, false);
        Document doc = JenkinsJobXml.loadManagedConfig(xml, JOB);
        assertNotNull(doc);
        String updated = JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, true);
        assertTrue(updated.contains("<disabled>true</disabled>"));
        assertFalse(updated.contains(script));
        // 再次解析后回填，验证转义往返无损
        Document reparsed = JenkinsJobXml.loadManagedConfig(updated, JOB);
        assertNotNull(reparsed);
        String roundTrip = JenkinsJobXml.applyFerryConfig(reparsed, script, false);
        assertTrue(roundTrip.contains("<disabled>false</disabled>"));
        assertTrue(roundTrip.contains("ls &lt;tmp&gt;"));
    }

    @Test
    void 更新仅改托管字段_人工追加的自定义参数与配置保留() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, false);
        // 人工追加一个自定义参数
        String withCustom = xml.replace("</parameterDefinitions>",
                "<hudson.model.StringParameterDefinition><name>CUSTOM_PARAM</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "</parameterDefinitions>");
        Document doc = JenkinsJobXml.loadManagedConfig(withCustom, JOB);
        assertNotNull(doc);
        String updated = JenkinsJobXml.applyFerryConfig(doc, "pipeline { agent any }", true);
        assertTrue(updated.contains("CUSTOM_PARAM"));
        assertTrue(updated.contains("<disabled>true</disabled>"));
        // Ferry 的 9 个参数仍完整（9 + 1 自定义 = 10 处开标签）
        assertEquals(10, updated.split("<hudson.model.StringParameterDefinition>", -1).length - 1);
    }

    @Test
    void 同步清理遗留的STAGE_SPEC参数() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, false);
        String legacy = xml.replace("</parameterDefinitions>",
                "<hudson.model.StringParameterDefinition><name>STAGE_SPEC</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "</parameterDefinitions>");
        Document doc = JenkinsJobXml.loadManagedConfig(legacy, JOB);
        assertNotNull(doc);
        String updated = JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, false);
        assertFalse(updated.contains("<name>STAGE_SPEC</name>"));
    }

    @Test
    void 非Ferry管理或类型不符的同名Job拒绝覆盖() {
        String foreign = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<flow-definition plugin=\"workflow-job\"><description>人工创建的 Job</description></flow-definition>";
        assertNull(JenkinsJobXml.loadManagedConfig(foreign, JOB));
        String freestyle = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<hudson.model.FreeStyleProject><description>Managed by Ferry: " + JOB + "</description></hudson.model.FreeStyleProject>";
        assertNull(JenkinsJobXml.loadManagedConfig(freestyle, JOB));
        String otherJob = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<flow-definition plugin=\"workflow-job\"><description>Managed by Ferry: other-job</description></flow-definition>";
        assertNull(JenkinsJobXml.loadManagedConfig(otherJob, JOB));
    }

    @Test
    void 带DOCTYPE的配置解析被拒绝_防XXE() {
        String xxe = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<flow-definition><description>Managed by Ferry: " + JOB + "&xxe;</description></flow-definition>";
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobXml.loadManagedConfig(xxe, JOB));
    }

    @Test
    void 缺脚本节点的配置拒绝更新() {
        String noScript = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<flow-definition plugin=\"workflow-job\"><description>Managed by Ferry: " + JOB + "</description></flow-definition>";
        Document doc = JenkinsJobXml.loadManagedConfig(noScript, JOB);
        assertNotNull(doc);
        assertThrows(JenkinsDispatchException.class, () -> JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, false));
    }
}
