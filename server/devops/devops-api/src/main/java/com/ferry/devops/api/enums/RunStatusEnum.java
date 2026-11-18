package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 运行状态
 */
@Getter
@AllArgsConstructor
public enum RunStatusEnum {
    QUEUED(1, "排队中"),
    RUNNING(2, "运行中"),
    BLOCKED(3, "阻塞"),
    SUCCESS(4, "成功"),
    FAILURE(5, "失败"),
    TIMEOUT(6, "超时"),
    CANCELLED(7, "已取消"),
    UNKNOWN(8, "结果待确认");

    private final Integer code;
    private final String desc;

    public static RunStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
