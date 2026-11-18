package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 触发结果
 */
@Getter
@AllArgsConstructor
public enum TriggerResultEnum {
    CREATED("CREATED", "新建运行"),
    MERGED("MERGED", "合并触发"),
    REUSED("REUSED", "复用运行"),
    REJECTED("REJECTED", "拒绝");

    private final String code;
    private final String desc;

    public static TriggerResultEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(String code) {
        return of(code) != null;
    }
}
