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
 * YAML 模式显式校验不通过（此前是假通过：validate 放行但保存/预览必然失败）；
 * 真实 YAML 解析可在引入依赖后替换本类。
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
     * 结构化校验（对外校验端点）：阶段 1–8、类型唯一、任务 0–30、超时 60–86400、执行器任务要求。
     *
     * <p>阶段允许零任务（画布可先建空阶段再补任务）；空阶段/空任务的完整性与可执行性
     * 交由运行触发预校验（RunServiceImpl#doPrecheck）提示，不在保存阶段拦截。
     */
    static ValidateResultVO validate(String definition, Integer mode) {
        // YAML 未支持：显式不通过，避免“校验通过但保存/预览必然失败”的假象
        if (Integer.valueOf(2).equals(mode)) {
            ValidateResultVO rejected = new ValidateResultVO();
            rejected.setValid(false);
            rejected.setErrors(List.of(error(0, "YAML 模式暂不支持，请使用表单（JSON）模式")));
            return rejected;
        }
        PipelineDefinitionDTO dto;
        try {
            dto = decode(definition);
        } catch (ServiceException e) {
            ValidateResultVO result = new ValidateResultVO();
            result.setValid(false);
            result.setErrors(List.of(error(0, e.getMessage())));
            return result;
        }
        return validateDto(dto, true);
    }

    /**
     * 结构化校验（已解码对象）：保存链路复用，allowEmpty=true 时放开阶段数量下限以支持显式清空定义。
     */
    static ValidateResultVO validateDto(PipelineDefinitionDTO dto, boolean requireNonEmpty) {
        ValidateResultVO result = new ValidateResultVO();
        List<ValidateResultVO.ValidateError> errors = new ArrayList<>();
        List<com.ferry.devops.api.dto.PipelineStageDTO> stages = dto.getStages();
        if (stages == null || stages.size() > 8 || (requireNonEmpty && stages.isEmpty())) {
            errors.add(error(0, requireNonEmpty ? "阶段数量须在 1–8 之间" : "阶段数量不能超过 8"));
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
                    boolean hasJenkins = tasks.stream().anyMatch(t -> "jenkins".equalsIgnoreCase(t.getExecutor()));
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
