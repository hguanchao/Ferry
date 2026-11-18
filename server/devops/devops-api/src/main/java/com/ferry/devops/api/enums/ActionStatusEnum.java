package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI动作状态
 */
@Getter
@AllArgsConstructor
public enum ActionStatusEnum {
    PROPOSED(1, "已提议"),
    PENDING_CONFIRM(2, "待确认"),
    AUTHORIZED(3, "已授权"),
    RUNNING(4, "执行中"),
    SUCCESS(5, "执行成功"),
    FAILURE(6, "执行失败"),
    REVOKED(7, "已撤销"),
    REJECTED(8, "已拒绝");

    private final Integer code;
    private final String desc;

    public static ActionStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
