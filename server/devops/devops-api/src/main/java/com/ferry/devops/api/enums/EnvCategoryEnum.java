package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 环境类别
 */
@Getter
@AllArgsConstructor
public enum EnvCategoryEnum {
    TEST(1, "测试"),
    STAGING(2, "预发"),
    PRODUCTION(3, "生产");

    private final Integer code;
    private final String desc;

    public static EnvCategoryEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
