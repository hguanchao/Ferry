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
 * Jenkins Job 配置 XML 测试：最小配置生成、托管字段更新保留无关配置、GitHub 项目属性托管、
 * 归属校验与 XXE 加固。
 */
class JenkinsJobXmlTest {

    private static final String JOB = "order-service";
    private static final String REPO_URL = "https://github.com/yangzongzhuan/RuoYi";
    private static final String REPO_NAME = "订单服务";
    private static final String SCRIPT_V1 = "pipeline {\n    agent any\n}";

    @Test
    void 最小配置包含归属标记_八个参数_禁用位_且不含回调参数与STAGE_SPEC() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, true, null, null);
        assertTrue(xml.contains("Managed by Ferry: " + JOB));
        assertTrue(xml.contains("order-service"));
        assertTrue(xml.contains("<disabled>true</disabled>"));
        assertEquals(8, xml.split("<hudson.model.StringParameterDefinition>", -1).length - 1);
        assertTrue(xml.contains("GIT_CREDENTIALS_ID"));
        assertFalse(xml.contains("CALLBACK_URL"));
        assertFalse(xml.contains("STAGE_MAP"));
        assertFalse(xml.contains("RUN_ID"));
        assertFalse(xml.contains("STAGE_SPEC"));
        // 未传仓库主页时不生成 GitHub 项目属性
        assertFalse(xml.contains("GitHubProjectProperty"));
        // 生成的配置可被安全解析并验证归属
        Document doc = JenkinsJobXml.loadManagedConfig(xml, JOB);
        assertNotNull(doc);
    }

    @Test
    void 传入仓库主页与显示名称时生成GitHub项目属性() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, true, REPO_URL, REPO_NAME);
        assertTrue(xml.contains("hudson.plugins.github.GitHubProjectProperty"));
        assertTrue(xml.contains("<projectUrl>" + REPO_URL + "</projectUrl>"));
        assertTrue(xml.contains("<displayName>" + REPO_NAME + "</displayName>"));
        // 属性节点位于 properties 内且可被安全解析
        Document doc = JenkinsJobXml.loadManagedConfig(xml, JOB);
        assertNotNull(doc);
    }

    @Test
    void GitHub项目属性被托管更新与移除() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, false, REPO_URL, REPO_NAME);
        Document doc = JenkinsJobXml.loadManagedConfig(xml, JOB);
        assertNotNull(doc);
        // 更新：url 与显示名称同步变化
        String updated = JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, false,
                "https://gitee.com/y_project/RuoYi", "订单服务-Gitee");
        assertTrue(updated.contains("<projectUrl>https://gitee.com/y_project/RuoYi</projectUrl>"));
        assertTrue(updated.contains("<displayName>订单服务-Gitee</displayName>"));
        assertFalse(updated.contains(REPO_URL));
        // 清空 url：属性被移除
        Document reparsed = JenkinsJobXml.loadManagedConfig(updated, JOB);
        assertNotNull(reparsed);
        String removed = JenkinsJobXml.applyFerryConfig(reparsed, SCRIPT_V1, false, null, null);
        assertFalse(removed.contains("GitHubProjectProperty"));
        // 缺子节点的旧属性节点也能被兜底补齐更新
        String bare = removed.replace("</properties>",
                "<hudson.plugins.github.GitHubProjectProperty/></properties>");
        Document bareDoc = JenkinsJobXml.loadManagedConfig(bare, JOB);
        assertNotNull(bareDoc);
        String repaired = JenkinsJobXml.applyFerryConfig(bareDoc, SCRIPT_V1, false, REPO_URL, REPO_NAME);
        assertTrue(repaired.contains("<projectUrl>" + REPO_URL + "</projectUrl>"));
    }

    @Test
    void 脚本特殊字符经DOM转义_更新往返不丢失() {
        String script = "sh '''echo 'a' && ls <tmp> > out'''";
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", script, false, null, null);
        Document doc = JenkinsJobXml.loadManagedConfig(xml, JOB);
        assertNotNull(doc);
        String updated = JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, true, null, null);
        assertTrue(updated.contains("<disabled>true</disabled>"));
        assertFalse(updated.contains(script));
        // 再次解析后回填，验证转义往返无损
        Document reparsed = JenkinsJobXml.loadManagedConfig(updated, JOB);
        assertNotNull(reparsed);
        String roundTrip = JenkinsJobXml.applyFerryConfig(reparsed, script, false, null, null);
        assertTrue(roundTrip.contains("<disabled>false</disabled>"));
        assertTrue(roundTrip.contains("ls &lt;tmp&gt;"));
    }

    @Test
    void 更新仅改托管字段_人工追加的自定义参数与配置保留() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, false, null, null);
        // 人工追加一个自定义参数
        String withCustom = xml.replace("</parameterDefinitions>",
                "<hudson.model.StringParameterDefinition><name>CUSTOM_PARAM</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "</parameterDefinitions>");
        Document doc = JenkinsJobXml.loadManagedConfig(withCustom, JOB);
        assertNotNull(doc);
        String updated = JenkinsJobXml.applyFerryConfig(doc, "pipeline { agent any }", true, null, null);
        assertTrue(updated.contains("CUSTOM_PARAM"));
        assertTrue(updated.contains("<disabled>true</disabled>"));
        // Ferry 的 8 个参数仍完整（8 + 1 自定义 = 9 处开标签）
        assertEquals(9, updated.split("<hudson.model.StringParameterDefinition>", -1).length - 1);
    }

    @Test
    void 同步清理遗留的STAGE_SPEC与回调参数() {
        String xml = JenkinsJobXml.buildMinimalJobXml(JOB, "order-service", SCRIPT_V1, false, null, null);
        String legacy = xml.replace("</parameterDefinitions>",
                "<hudson.model.StringParameterDefinition><name>STAGE_SPEC</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "<hudson.model.StringParameterDefinition><name>RUN_ID</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "<hudson.model.StringParameterDefinition><name>CALLBACK_URL</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "<hudson.model.StringParameterDefinition><name>STAGE_MAP</name>"
                        + "<description/><defaultValue/><trim>false</trim></hudson.model.StringParameterDefinition>"
                        + "</parameterDefinitions>");
        Document doc = JenkinsJobXml.loadManagedConfig(legacy, JOB);
        assertNotNull(doc);
        String updated = JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, false, null, null);
        assertFalse(updated.contains("<name>STAGE_SPEC</name>"));
        assertFalse(updated.contains("<name>RUN_ID</name>"));
        assertFalse(updated.contains("<name>CALLBACK_URL</name>"));
        assertFalse(updated.contains("<name>STAGE_MAP</name>"));
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
        assertThrows(JenkinsDispatchException.class,
                () -> JenkinsJobXml.applyFerryConfig(doc, SCRIPT_V1, false, null, null));
    }
}
