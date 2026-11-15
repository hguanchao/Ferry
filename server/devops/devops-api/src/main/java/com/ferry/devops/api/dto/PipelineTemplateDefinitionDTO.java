package com.ferry.devops.api.dto;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.CollUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 流水线模板定义：阶段与占位变量
 */
@Data
public class PipelineTemplateDefinitionDTO {

    /**
     * 占位变量引用语法 {{变量名}}，允许花括号内留空白，与前端替换规则保持同一口径
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]{0,49})\\s*\\}\\}");

    @Schema(description = "阶段列表，数组顺序即执行顺序")
    @Valid
    private List<PipelineStageDTO> stages;

    @Schema(description = "占位变量列表")
    @Valid
    private List<TemplateVariableDTO> variables;

    /**
     * 校验模板定义
     *
     * <p>阶段结构复用流水线定义的同一套规则；占位变量做数量、重名与完整性校验，
     * 定义里出现的每一个变量引用都必须在变量表中声明。
     */
    public void validate() {
        new PipelineDefinitionDTO().validateStages(stages);
        validateVariables();
        validatePlaceholders();
    }

    /**
     * 校验占位变量表：数量上限与变量名唯一
     */
    private void validateVariables() {
        if (CollUtil.isEmpty(variables)) {
            return;
        }
        if (variables.size() > 20) {
            throw new ServiceException("占位变量不能超过 20 个");
        }
        Set<String> keys = new HashSet<>();
        for (TemplateVariableDTO variable : variables) {
            if (variable == null) {
                throw new ServiceException("占位变量配置不能为空");
            }
            if (!keys.add(variable.getKey())) {
                throw new ServiceException("占位变量名「" + variable.getKey() + "」重复");
            }
        }
    }

    /**
     * 校验占位变量完整性：扫描阶段下的所有文本，未声明的变量引用直接报错
     */
    private void validatePlaceholders() {
        Set<String> declared = CollUtil.isEmpty(variables) ? Set.of()
                : variables.stream().map(TemplateVariableDTO::getKey).collect(Collectors.toSet());
        for (PipelineStageDTO stage : stages) {
            String stageLabel = "阶段「" + safeName(stage.getName()) + "」";
            checkText(stageLabel, stage.getName(), declared);
            if (stage.getArtifacts() != null) {
                for (String artifact : stage.getArtifacts()) {
                    checkText(stageLabel + "的产出物", artifact, declared);
                }
            }
            if (stage.getTasks() == null) {
                continue;
            }
            for (PipelineTaskDTO task : stage.getTasks()) {
                String taskLabel = stageLabel + "任务「" + safeName(task.getName()) + "」";
                checkText(taskLabel, task.getName(), declared);
                checkText(taskLabel, task.getCommand(), declared);
            }
        }
    }

    /**
     * 检查一段文本里的变量引用是否都已声明
     *
     * @param where     出错时的定位描述
     * @param text      待检查文本
     * @param declared  已声明的变量名集合
     */
    private void checkText(String where, String text, Set<String> declared) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!declared.contains(key)) {
                throw new ServiceException(where + "使用了未声明的占位变量 {{" + key + "}}，请先在变量表中声明");
            }
        }
    }

    /**
     * 空名称时的定位兜底文案
     *
     * @param name 名称原文
     * @return 名称或「未命名」
     */
    private String safeName(String name) {
        return name == null || name.isBlank() ? "未命名" : name;
    }
}
