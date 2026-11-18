package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI输出类型
 */
@Getter
@AllArgsConstructor
public enum AiOutputTypeEnum {
    FAILURE_ATTRIBUTION(1, "失败归因"),
    ADMISSION(2, "准入辅助"),
    RELEASE_PLAN(3, "发布预案"),
    INCIDENT(4, "异常归因");

    private final Integer code;
    private final String desc;

    public static AiOutputTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
