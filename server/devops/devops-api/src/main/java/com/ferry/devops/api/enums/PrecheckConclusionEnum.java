package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 预校验结论
 *
 * <p>「不适用」与「不通过」是两种结论：前者放行（如分支级门禁未启用），后者拦截。
 */
@Getter
@AllArgsConstructor
public enum PrecheckConclusionEnum {
    PASS("PASS", "通过"),
    FAIL("FAIL", "不通过"),
    NA("NA", "不适用");

    private final String code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 预校验结论取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static PrecheckConclusionEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的预校验结论
     *
     * @param code 预校验结论取值
     * @return 是否已定义
     */
    public static boolean contains(String code) {
        return of(code) != null;
    }
}
