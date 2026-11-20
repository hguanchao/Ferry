package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.vo.PipelinePreviewVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线脚本渲染：触发时的 STAGE_SPEC 与配置页的脚本预览共用同一份阶段筛选逻辑。
 */
final class PipelineScriptRenderer {

    private PipelineScriptRenderer() {
    }

    /**
     * 把流水线版本的 CI 阶段与任务脚本序列化为 Jenkins 参数。
     */
    static String buildStageSpec(PipelineDefinitionDTO definition) {
        List<Map<String, Object>> stages = new ArrayList<>();
        if (definition != null && definition.getStages() != null) {
            for (PipelineStageDTO stage : definition.getStages()) {
                if (stage.getType() == null || stage.getType() > 4) {
                    continue;
                }
                Map<String, Object> stageSpec = new LinkedHashMap<>();
                stageSpec.put("type", stage.getType());
                stageSpec.put("name", stage.getName());
                stageSpec.put("buildTool", stage.getBuildTool());
                stageSpec.put("timeoutSeconds", stage.getTimeoutSeconds());
                stageSpec.put("outputs", stage.getOutputs());
                List<Map<String, Object>> tasks = new ArrayList<>();
                if (stage.getTasks() != null) {
                    for (PipelineTaskDTO task : stage.getTasks()) {
                        if (task.getExecutor() != null && !"jenkins".equalsIgnoreCase(task.getExecutor())) {
                            continue;
                        }
                        Map<String, Object> taskSpec = new LinkedHashMap<>();
                        taskSpec.put("name", task.getName());
                        taskSpec.put("scriptRef", task.getScriptRef());
                        taskSpec.put("scriptContent", task.getScriptContent());
                        taskSpec.put("params", task.getParams());
                        tasks.add(taskSpec);
                    }
                }
                stageSpec.put("tasks", tasks);
                stages.add(stageSpec);
            }
        }
        return JsonUtil.toJson(stages);
    }

    /**
     * 按当前定义渲染一段可读的 Jenkins 脚本预览，并收集空命令等告警。
     */
    static PipelinePreviewVO preview(PipelineDefinitionDTO definition) {
        List<String> warnings = new ArrayList<>();
        StringBuilder script = new StringBuilder();
        script.append("// Ferry 流水线脚本预览：仅展示 CI 阶段，复制到 Jenkins 前请确认命令与工具链\n");
        script.append("node {\n");
        script.append("    currentBuild.displayName = \"#${env.BUILD_NUMBER} ${params.RUN_ID}\"\n");
        script.append("    stage('Checkout') {\n");
        script.append("        // checkout REPO_URL，并 git checkout COMMIT_ID\n");
        script.append("    }\n");

        List<PipelineStageDTO> stages = definition == null ? null : definition.getStages();
        boolean hasCiStage = false;
        if (stages != null) {
            for (PipelineStageDTO stage : stages) {
                if (stage.getType() == null || stage.getType() > 4) {
                    continue;
                }
                hasCiStage = true;
                script.append("\n    stage('").append(escapeSingleQuoted(stage.getName())).append("') {\n");
                if (StrUtil.isNotBlank(stage.getBuildTool())) {
                    script.append("        // 构建工具: ").append(stage.getBuildTool()).append("\n");
                }
                int timeout = stage.getTimeoutSeconds() == null ? 1800 : stage.getTimeoutSeconds();
                script.append("        timeout(time: ").append(timeout).append(", unit: 'SECONDS') {\n");
                boolean hasJenkinsTask = false;
                if (stage.getTasks() != null) {
                    for (PipelineTaskDTO task : stage.getTasks()) {
                        if (task.getExecutor() != null && !"jenkins".equalsIgnoreCase(task.getExecutor())) {
                            continue;
                        }
                        hasJenkinsTask = true;
                        script.append("            // 任务: ").append(task.getName()).append("\n");
                        if (StrUtil.isNotBlank(task.getScriptContent())) {
                            if (task.getScriptContent().contains("'''")) {
                                warnings.add("阶段「" + stage.getName() + "」任务「" + task.getName()
                                        + "」命令包含 '''，预览无法完整展示");
                                script.append("            // 命令包含 \"'''\"，请改用脚本文件或拆分命令\n");
                            } else {
                                script.append("            sh '''").append(task.getScriptContent()).append("'''\n");
                            }
                        } else if (StrUtil.isNotBlank(task.getScriptRef())) {
                            warnings.add("阶段「" + stage.getName() + "」任务「" + task.getName()
                                    + "」只配置了 scriptRef，当前不会自动加载执行");
                            script.append("            // 未配置执行命令，scriptRef=")
                                    .append(task.getScriptRef()).append("\n");
                        } else {
                            warnings.add("阶段「" + stage.getName() + "」任务「" + task.getName()
                                    + "」未配置执行命令");
                            script.append("            // 未配置执行命令，当前不会执行任何内容\n");
                        }
                    }
                }
                if (!hasJenkinsTask) {
                    warnings.add("阶段「" + stage.getName() + "」没有 Jenkins 任务");
                }
                if (stage.getType() == 4) {
                    if (stage.getOutputs() == null || stage.getOutputs().isEmpty()) {
                        warnings.add("制品入库阶段「" + stage.getName() + "」未配置产出物路径");
                    } else {
                        for (String output : stage.getOutputs()) {
                            script.append("            archiveArtifacts artifacts: '")
                                    .append(escapeSingleQuoted(output))
                                    .append("', allowEmptyArchive: true\n");
                        }
                    }
                }
                script.append("        }\n");
                script.append("    }\n");
            }
        }
        if (!hasCiStage) {
            warnings.add("当前定义没有可下发的 CI 阶段（构建/单元测试/静态扫描/制品入库）");
        }
        script.append("}\n");

        PipelinePreviewVO vo = new PipelinePreviewVO();
        vo.setScript(script.toString());
        vo.setStageSpec(buildStageSpec(definition));
        vo.setWarnings(warnings);
        return vo;
    }

    private static String escapeSingleQuoted(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
