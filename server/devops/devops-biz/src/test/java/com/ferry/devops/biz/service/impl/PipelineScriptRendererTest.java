package com.ferry.devops.biz.service.impl;

import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.vo.PipelinePreviewVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 脚本渲染测试：完整声明式结构、转义正确性、空定义与预览同源性。
 */
class PipelineScriptRendererTest {

    private PipelineTaskDTO task(String name, String executor, String scriptRef, String scriptContent) {
        PipelineTaskDTO task = new PipelineTaskDTO();
        task.setName(name);
        task.setExecutor(executor);
        task.setScriptRef(scriptRef);
        task.setScriptContent(scriptContent);
        return task;
    }

    private PipelineStageDTO stage(int type, String name, String buildTool, Integer timeoutSeconds,
                                   List<PipelineTaskDTO> tasks, List<String> outputs) {
        PipelineStageDTO stage = new PipelineStageDTO();
        stage.setType(type);
        stage.setName(name);
        stage.setBuildTool(buildTool);
        stage.setTimeoutSeconds(timeoutSeconds);
        stage.setTasks(tasks);
        stage.setOutputs(outputs);
        return stage;
    }

    private PipelineDefinitionDTO definition(PipelineStageDTO... stages) {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        definition.setStages(new ArrayList<>(List.of(stages)));
        return definition;
    }

    @Test
    void 渲染完整pipeline包含检出与CI阶段_非jenkins任务与非CI阶段不进脚本() {
        PipelineDefinitionDTO definition = definition(
                stage(1, "构建", "Maven", 600, List.of(task("打包", "jenkins", null, "mvn -B package")), null),
                stage(2, "单元测试", null, null, List.of(task("单测", "jenkins", null, "mvn test")), null),
                stage(5, "部署非生产", null, null, List.of(task("部署", "deployer", null, "deploy.sh")), null));
        String script = PipelineScriptRenderer.render(definition);
        assertTrue(script.contains("pipeline {"));
        assertTrue(script.contains("agent any"));
        // 纯轮询：脚本不注入任何回调函数、回调地址或阶段映射
        assertFalse(script.contains("ferryNotify"));
        assertFalse(script.contains("CALLBACK_URL"));
        assertFalse(script.contains("STAGE_MAP"));
        assertFalse(script.contains("RUN_ID"));
        assertFalse(script.contains("withCredentials"));
        assertFalse(script.contains("curl"));
        // 代码检出使用 Job 参数，运行时注入
        assertTrue(script.contains("stage('Checkout')"));
        assertTrue(script.contains("params.REPO_URL"));
        assertTrue(script.contains("params.COMMIT_ID"));
        // 检出语句括号必须配平：曾因少一个 ] 导致整段脚本 Groovy 编译失败（Jenkins 侧构建未跑即挂）
        assertTrue(script.contains("credentialsId: (params.GIT_CREDENTIALS_ID ?: '')]]])"));
        assertTrue(script.contains("stage('构建')"));
        assertTrue(script.contains("stage('单元测试')"));
        assertTrue(script.contains("sh '''mvn -B package'''"));
        // 未显式配置超时时使用默认 1800
        assertTrue(script.contains("timeout(time: 1800, unit: 'SECONDS')"));
        assertTrue(script.contains("timeout(time: 600, unit: 'SECONDS')"));
        // 非 CI 阶段与非 jenkins 执行器任务不下发
        assertFalse(script.contains("deploy.sh"));
        assertFalse(script.contains("部署非生产"));
        assertFalse(script.contains("STAGE_SPEC"));
    }

    @Test
    void 命令中的单引号与反斜杠被转义且多行命令原样保留() {
        PipelineDefinitionDTO definition = definition(
                stage(1, "构建", null, null, List.of(task("打包", "jenkins", null, "echo 'a\\b'")), null));
        String script = PipelineScriptRenderer.render(definition);
        assertTrue(script.contains("sh '''echo \\'a\\\\b\\'''"));
        PipelineDefinitionDTO multi = definition(
                stage(1, "构建", null, null, List.of(task("打包", "jenkins", null, "set -e\nmvn package")), null));
        assertTrue(PipelineScriptRenderer.render(multi).contains("sh '''set -e\nmvn package'''"));
    }

    @Test
    void 任务名注释压成单行防止换行注入() {
        PipelineDefinitionDTO definition = definition(
                stage(1, "构建", null, null, List.of(task("x\nsh '''evil'''", "jenkins", null, "echo hi")), null));
        String script = PipelineScriptRenderer.render(definition);
        assertTrue(script.contains("// 任务: x sh '''evil'''"));
        assertFalse(script.contains("// 任务: x\n"));
    }

    @Test
    void 制品入库阶段输出archiveArtifacts_buildTool仅作为注释() {
        PipelineDefinitionDTO definition = definition(
                stage(4, "制品入库", "Maven", null, List.of(task("归档", "jenkins", null, "ls target")), List.of("target/*.jar")));
        String script = PipelineScriptRenderer.render(definition);
        assertTrue(script.contains("archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true"));
        assertTrue(script.contains("// 构建工具: Maven"));
    }

    @Test
    void 空定义渲染最小脚本_预览告警提示无可下发阶段() {
        String script = PipelineScriptRenderer.render(new PipelineDefinitionDTO());
        assertFalse(script.contains("stage("));
        assertTrue(script.contains("agent any"));
        PipelinePreviewVO vo = PipelineScriptRenderer.preview(new PipelineDefinitionDTO());
        assertTrue(vo.getWarnings().stream().anyMatch(w -> w.contains("没有可下发的 CI 阶段")));
    }

    @Test
    void 预览脚本与同步脚本同源_仅附加告警() {
        PipelineDefinitionDTO definition = definition(
                stage(1, "构建", null, null,
                        List.of(task("打包", "jenkins", null, "mvn package"),
                                task("空任务", "jenkins", null, null)),
                        null));
        PipelinePreviewVO vo = PipelineScriptRenderer.preview(definition);
        assertEquals(PipelineScriptRenderer.render(definition), vo.getScript());
        assertTrue(vo.getWarnings().stream().anyMatch(w -> w.contains("未配置执行命令")));
        assertFalse(vo.getWarnings().isEmpty());
    }

    @Test
    void scriptRef任务生成提示注释并告警() {
        PipelineDefinitionDTO definition = definition(
                stage(1, "构建", null, null, List.of(task("打包", "jenkins", "script/build.sh", null)), null));
        PipelinePreviewVO vo = PipelineScriptRenderer.preview(definition);
        assertTrue(vo.getScript().contains("// 仅配置 scriptRef: script/build.sh"));
        assertTrue(vo.getWarnings().stream().anyMatch(w -> w.contains("只配置了 scriptRef")));
    }

    @Test
    void 渲染脚本整体括号配平_防止注入块引入语法回归() {
        PipelineDefinitionDTO definition = definition(
                stage(1, "构建", "Maven", null, List.of(task("打包", "jenkins", null, "mvn -B package")), null),
                stage(2, "单元测试", null, null, List.of(task("单测", "jenkins", null, "mvn test")), null));
        String script = PipelineScriptRenderer.render(definition);
        // 括号守恒是脚本可编译的必要条件；此前 Checkout 曾因少一个 ] 导致整段脚本编译失败
        assertEquals(0, count(script, '(') - count(script, ')'));
        assertEquals(0, count(script, '[') - count(script, ']'));
        assertEquals(0, count(script, '{') - count(script, '}'));
        assertFalse(script.contains("ferryNotify"));
    }

    private static long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
