package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 门禁规则作用域。优先级：分支 > 应用 > 组织。
 */
@Getter
@AllArgsConstructor
public enum GateScopeEnum {
    ORGANIZATION(1, "组织", 1),
    APPLICATION(2, "应用", 2),
    BRANCH(3, "分支", 3);

    private final Integer code;
    private final String desc;
    /** 优先级，越大越高 */
    private final Integer priority;

    public static GateScopeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
