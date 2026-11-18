package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI动作级别
 */
@Getter
@AllArgsConstructor
public enum ActionLevelEnum {
    L0(0, "L0"),
    L1(1, "L1"),
    L2(2, "L2"),
    L3(3, "L3");

    private final Integer code;
    private final String desc;

    public static ActionLevelEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
