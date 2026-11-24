package com.ferry.devops.biz.utils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ferry.common.core.utils.JsonUtil;
import com.ferry.devops.api.entity.RunStageTask;
import com.ferry.devops.api.enums.StageStatusEnum;

/**
 * 阶段/任务执行结果摘要的构建工具。
 *
 * <p>写入 {@code dev_run_stage.result} / {@code dev_run_stage_task.result} 的结构化 JSON。
 * 阶段结果分两次写入：Jenkins 状态同步写执行结果，门禁裁决写门禁结果（gate 节点），
 * 因此必须用 {@link #merge(String, Map)} 按字段合并，避免互相覆盖。
 */
public final class StageResultUtil {

    private StageResultUtil() {
    }

    /**
     * 构建阶段执行结果并合并进已有摘要。
     *
     * @param existing 已有 result JSON（可能为 null 或门禁摘要）
     * @param status   阶段终态
     * @param start    开始时间
     * @param end      结束时间
     * @param extra    Jenkins 细节（jenkinsStage/status/durationMillis 等），可为 null
     */
    public static String execution(String existing, StageStatusEnum status, Date start, Date end,
                                   Map<String, Object> extra) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (status != null) {
            fields.put("status", status.name());
            fields.put("desc", status.getDesc());
        }
        putTime(fields, "startTime", start);
        putTime(fields, "endTime", end);
        if (start != null && end != null) {
            fields.put("durationMs", Math.max(0, end.getTime() - start.getTime()));
        }
        if (extra != null) {
            fields.putAll(extra);
        }
        return merge(existing, fields);
    }

    /**
     * 构建任务执行结果（任务无已有摘要，直接生成）。
     *
     * @param status   任务终态
     * @param task     任务实体，取 executor/scriptRef/scriptContent
     * @param time     状态落定时间
     * @param extra    Jenkins 细节，可为 null
     */
    public static String task(StageStatusEnum status, RunStageTask task, Date time, Map<String, Object> extra) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (status != null) {
            fields.put("status", status.name());
            fields.put("desc", status.getDesc());
        }
        if (task != null) {
            putIfPresent(fields, "executor", task.getExecutor());
            putIfPresent(fields, "scriptRef", task.getScriptRef());
        }
        putTime(fields, "time", time);
        if (extra != null) {
            fields.putAll(extra);
        }
        return JsonUtil.toJson(fields);
    }

    /**
     * 把门禁摘要作为 {@code gate} 节点合并进阶段结果，保留已有执行结果。
     */
    public static String gate(String existing, Map<String, Object> gateSummary) {
        return merge(existing, Map.of("gate", gateSummary));
    }

    /**
     * 按字段合并进已有 JSON：保留已有节点，同名字段以新值覆盖；非法 JSON 视为空重新构建。
     */
    @SuppressWarnings("unchecked")
    private static String merge(String existing, Map<String, Object> fields) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existing != null && !existing.isBlank()) {
            try {
                Map<String, Object> parsed = JsonUtil.toBean(existing, Map.class);
                if (parsed != null) {
                    merged.putAll(parsed);
                }
            } catch (RuntimeException e) {
                // 历史脏数据无法解析时丢弃，按本次字段重建
            }
        }
        merged.putAll(fields);
        return JsonUtil.toJson(merged);
    }

    private static void putTime(Map<String, Object> fields, String key, Date time) {
        if (time != null) {
            fields.put(key, time.toInstant().toString());
        }
    }

    private static void putIfPresent(Map<String, Object> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, value);
        }
    }
}
