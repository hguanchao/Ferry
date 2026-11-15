package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水线运行状态
 *
 * <p>状态机见需求整体架构：排队中/运行中为进行态，成功/失败/超时/已取消为终态，阻塞等待人工豁免或继续。
 */
@Getter
@AllArgsConstructor
public enum RunStatusEnum {
    QUEUED("QUEUED", "排队中"),
    RUNNING("RUNNING", "运行中"),
    BLOCKED("BLOCKED", "阻塞"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    TIMEOUT("TIMEOUT", "超时"),
    CANCELED("CANCELED", "已取消");

    private final String code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 运行状态取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static RunStatusEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的运行状态
     *
     * @param code 运行状态取值
     * @return 是否已定义
     */
    public static boolean contains(String code) {
        return of(code) != null;
    }
}
