package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 准入检查人工结论
 */
@Getter
@AllArgsConstructor
public enum CheckConclusionEnum {
    PASS(1, "通过"),
    FAIL(2, "不通过"),
    NOT_APPLICABLE(3, "不适用"),
    ACKNOWLEDGED(4, "已知悉");

    private final Integer code;
    private final String desc;

    public static CheckConclusionEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
