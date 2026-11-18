package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI 动作来源
 */
@Getter
@AllArgsConstructor
public enum ActionSourceEnum {
    ATTRIBUTION(1, "归因"),
    PLAN(2, "预案"),
    INCIDENT(3, "异常");

    private final Integer code;
    private final String desc;

    public static ActionSourceEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
