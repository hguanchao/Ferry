package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.biz.gate.GateReportContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 流水线脚本渲染：把三表定义渲染为完整声明式 Jenkins pipeline 脚本。
 *
 * <p>配置页预览与 Job 同步共用 {@link #render(PipelineDefinitionDTO)}，保证“预览即所得”；
 * 渲染结果自包含（checkout 引用 Job 参数，阶段命令静态展开），不再使用 STAGE_SPEC 动态解释协议。
 *
 * <p>状态与结果同步统一由 Ferry 轮询 Jenkins API、拉取归档产物完成，脚本不注入任何回调逻辑。
 */
final class PipelineScriptRenderer {

    /** 阶段超时默认值（秒），与画布编辑器默认一致 */
    private static final int DEFAULT_STAGE_TIMEOUT_SECONDS = 1800;

    /** 参与 Jenkins 执行的 CI 阶段类型上限：1构建 2单测 3静态扫描 4制品入库；5-8 属部署/审批域，不在本次迁入范围 */
    private static final int CI_STAGE_TYPE_MAX = 4;

    private PipelineScriptRenderer() {
    }

    /**
     * 渲染完整声明式脚本，预览与 Job 同步共用同一结果。
     *
     * <p>空定义（无 CI 阶段）渲染为注释 + 最小可解析脚本，配合 Job 禁用位表达“待配置”。
     */
    static String render(PipelineDefinitionDTO definition) {
        List<PipelineStageDTO> ciStages = selectCiStages(definition);
        StringBuilder script = new StringBuilder();
        script.append("// 本脚本由 Ferry 按流水线定义自动生成，请勿在 Jenkins 侧手工编辑；修改请回到流水线画布保存\n");
        if (ciStages.isEmpty()) {
            script.append("// 尚未配置任何 CI 阶段，配置完成后回到 Ferry 重新保存同步\n");
            script.append("pipeline {\n");
            script.append("    agent any\n");
            script.append("}\n");
            return script.toString();
        }
        script.append("pipeline {\n");
        script.append("    agent any\n");
        script.append("    stages {\n");
        appendCheckoutStage(script);
        for (PipelineStageDTO stage : ciStages) {
            appendCiStage(script, stage);
        }
        script.append("    }\n");
        script.append("}\n");
        return script.toString();
    }

    /**
     * 筛选参与 Jenkins 执行的 CI 阶段（type 1-4）。
     */
    private static List<PipelineStageDTO> selectCiStages(PipelineDefinitionDTO definition) {
        List<PipelineStageDTO> ciStages = new ArrayList<>();
        if (definition != null && definition.getStages() != null) {
            for (PipelineStageDTO stage : definition.getStages()) {
                if (stage.getType() != null && stage.getType() <= CI_STAGE_TYPE_MAX) {
                    ciStages.add(stage);
                }
            }
        }
        return ciStages;
    }

    /**
     * 代码检出阶段：仓库地址、提交与凭据均来自 Job 参数，触发时注入。
     */
    private static void appendCheckoutStage(StringBuilder script) {
        script.append("        stage('Checkout') {\n");
        script.append("            steps {\n");
        script.append("                checkout([$class: 'GitSCM',\n");
        script.append("                    branches: [[name: params.COMMIT_ID]],\n");
        script.append("                    userRemoteConfigs: [[\n");
        script.append("                        url: params.REPO_URL,\n");
        script.append("                        credentialsId: (params.GIT_CREDENTIALS_ID ?: '')]]])\n");
        script.append("            }\n");
        script.append("        }\n");
    }

    private static void appendCiStage(StringBuilder script, PipelineStageDTO stage) {
        script.append("        stage('").append(escapeSingleQuoted(stage.getName())).append("') {\n");
        if (StrUtil.isNotBlank(stage.getBuildTool())) {
            // buildTool 仅作提示注释，工具环境由执行节点提供，不在此安装/切换
            script.append("            // 构建工具: ").append(singleLine(stage.getBuildTool())).append("\n");
        }
        int timeout = stage.getTimeout() == null ? DEFAULT_STAGE_TIMEOUT_SECONDS : stage.getTimeout();
        script.append("            options {\n");
        script.append("                timeout(time: ").append(timeout).append(", unit: 'SECONDS')\n");
        script.append("            }\n");
        script.append("            steps {\n");
        if (stage.getTasks() != null) {
            for (PipelineStageTaskDTO task : stage.getTasks()) {
                if (task.getExecutor() != null && !IntConst.ONE.equals(task.getExecutor())) {
                    continue;
                }
                appendTask(script, task);
            }
        }
        if (stage.getType() == 2) {
            script.append("                // 单测归一化报告：Ferry 拉取归档后做门禁判定\n");
            script.append("                archiveArtifacts artifacts: '")
                    .append(GateReportContract.UNIT_TEST_REPORT_PATH)
                    .append("', allowEmptyArchive: true\n");
        }
        if (stage.getType() == 3) {
            script.append("                // 静态扫描归一化报告：Ferry 拉取归档后做门禁判定\n");
            script.append("                archiveArtifacts artifacts: '")
                    .append(GateReportContract.SCAN_REPORT_PATH)
                    .append("', allowEmptyArchive: true\n");
        }
        if (stage.getType() == 4) {
            for (String output : splitOutputs(stage.getOutputs())) {
                script.append("                archiveArtifacts artifacts: '")
                        .append(escapeSingleQuoted(output))
                        .append("', allowEmptyArchive: true\n");
            }
        }
        script.append("            }\n");
        script.append("        }\n");
    }

    private static void appendTask(StringBuilder script, PipelineStageTaskDTO task) {
        script.append("                // 任务: ").append(singleLine(task.getName())).append("\n");
        if (StrUtil.isNotBlank(task.getScriptContent())) {
            script.append("                sh '''")
                    .append(escapeSingleQuoted(task.getScriptContent()))
                    .append("'''\n");
        } else if (StrUtil.isNotBlank(task.getScriptRef())) {
            // scriptRef 只是源码内脚本路径提示，生成的脚本不动态加载执行
            script.append("                // 仅配置 scriptRef: ").append(singleLine(task.getScriptRef()))
                    .append("，不自动加载执行\n");
        } else {
            script.append("                // 未配置执行命令，本任务不执行任何内容\n");
        }
    }

    private static List<String> splitOutputs(String outputs) {
        if (StrUtil.isBlank(outputs)) {
            return List.of();
        }
        return Arrays.stream(outputs.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /** Groovy 单引号字符串转义：反斜杠与单引号 */
    private static String escapeSingleQuoted(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** 注释内容压成单行，防止用户输入的换行符破坏 // 注释注入代码 */
    private static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("\\s*\\r?\\n\\s*", " ");
    }
}
