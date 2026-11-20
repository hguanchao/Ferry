package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水线状态（含 Jenkins 同步态，复用 dev_pipeline.status 字段，无独立同步状态列）
 *
 * <p>状态机规则（同步编排见 PipelineJobSyncService）：
 * <ul>
 *   <li>触发：仅 {@link #ENABLED} 可运行；待配置/同步失败/停用均拦截</li>
 *   <li>同步失败：落 {@link #SYNC_FAILED}，唯一例外是停用操作——保持 {@link #DISABLED}
 *       （Ferry 侧已是安全态，Job 侧禁用位由下次成功同步补齐）</li>
 *   <li>同步成功：{@link #PENDING_CONFIG}→{@link #ENABLED}、{@link #SYNC_FAILED}→{@link #ENABLED}，
 *       {@link #DISABLED}/{@link #ENABLED} 保持</li>
 *   <li>空定义（新建未配置/显式清空）：同步空脚本并禁用 Job，落 {@link #PENDING_CONFIG}</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum PipelineStatusEnum {
    DISABLED(0, "停用"),
    ENABLED(1, "启用"),
    PENDING_CONFIG(2, "待配置"),
    SYNC_FAILED(3, "同步失败");

    private final Integer code;
    private final String desc;

    public static PipelineStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
