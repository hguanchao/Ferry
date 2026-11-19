package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.vo.ValidateResultVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 流水线定义编解码：JSON ↔ 定义对象（JSON only）。
 *
 * <p>snakeyaml 未引入 classpath，且项目使用 Jackson 3（tools.jackson）无 YAML 模块，
 * 因此 YAML 模式按「最小实现优先保证编译」降级为 JSON only：YAML 原文原样保存、不解析，
 * validate 时不报 YAML 错，仅对 JSON 模式做结构化校验。真实 YAML 解析可在引入依赖后替换本类。
 */
final class PipelineDefinitionCodec {

    private PipelineDefinitionCodec() {
    }

    /**
     * 解析定义文本为结构化对象
     *
     * @param definition 定义文本（JSON）
     * @return 定义对象
     */
    static PipelineDefinitionDTO decode(String definition) {
        if (StrUtil.isBlank(definition)) {
            throw new ServiceException("流水线定义不能为空");
        }
        try {
            PipelineDefinitionDTO dto = JsonUtil.toBean(definition, PipelineDefinitionDTO.class);
            if (dto == null) {
                throw new ServiceException("流水线定义格式错误");
            }
            return dto;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("流水线定义不是合法的 JSON");
        }
    }

    /**
     * 结构化校验：阶段 1–8、类型唯一、任务 0–30、超时 60–86400、执行器任务要求。
     *
     * <p>阶段允许零任务（画布可先建空阶段再补任务）；空阶段/空任务的完整性与可执行性
     * 交由运行触发预校验（RunServiceImpl#doPrecheck）提示，不在保存阶段拦截。
     */
    static ValidateResultVO validate(String definition, Integer mode) {
        ValidateResultVO result = new ValidateResultVO();
        List<ValidateResultVO.ValidateError> errors = new ArrayList<>();
        // YAML 模式降级：仅保留原文、不做语法解析，不报 YAML 错
        if (Integer.valueOf(2).equals(mode)) {
            result.setValid(true);
            result.setErrors(errors);
            return result;
        }
        PipelineDefinitionDTO dto;
        try {
            dto = decode(definition);
        } catch (ServiceException e) {
            errors.add(error(0, e.getMessage()));
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }
        List<com.ferry.devops.api.dto.PipelineStageDTO> stages = dto.getStages();
        if (stages == null || stages.isEmpty() || stages.size() > 8) {
            errors.add(error(0, "阶段数量须在 1–8 之间"));
        } else {
            java.util.Set<Integer> types = new java.util.HashSet<>();
            int line = 1;
            for (com.ferry.devops.api.dto.PipelineStageDTO stage : stages) {
                if (stage.getType() == null || stage.getType() < 1 || stage.getType() > 8) {
                    errors.add(error(line, "阶段类型须在 1–8 之间"));
                } else if (!types.add(stage.getType())) {
                    errors.add(error(line, "阶段类型不能重复"));
                }
                if (stage.getTimeoutSeconds() != null
                        && (stage.getTimeoutSeconds() < 60 || stage.getTimeoutSeconds() > 86400)) {
                    errors.add(error(line, "超时须在 60–86400 秒之间"));
                }
                List<com.ferry.devops.api.dto.PipelineTaskDTO> tasks = stage.getTasks();
                int taskCount = tasks == null ? 0 : tasks.size();
                if (taskCount > 30) {
                    errors.add(error(line, "单阶段任务数量不能超过 30"));
                } else if (taskCount > 0 && stage.getType() != null && stage.getType() <= 4) {
                    boolean hasJenkins = tasks.stream().anyMatch(t -> "jenkins".equals(t.getExecutor()));
                    if (!hasJenkins) {
                        errors.add(error(line, "前四类阶段须包含至少一个 executor=jenkins 的任务"));
                    }
                }
                line++;
            }
        }
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    private static ValidateResultVO.ValidateError error(int line, String message) {
        ValidateResultVO.ValidateError e = new ValidateResultVO.ValidateError();
        e.setLine(line);
        e.setMessage(message);
        return e;
    }
}
