package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 基线问题状态。
 */
@Getter
@AllArgsConstructor
public enum BaselineIssueStatusEnum {
    LEGACY(1, "遗留"),
    FIXED(2, "已修复"),
    FALSE_POSITIVE(3, "误报");

    private final Integer code;
    private final String desc;

    public static BaselineIssueStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
