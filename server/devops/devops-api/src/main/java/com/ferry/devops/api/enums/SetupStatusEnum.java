package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 接入完善度
 */
@Getter
@AllArgsConstructor
public enum SetupStatusEnum {
    INCOMPLETE(1, "待完善"),
    COMPLETE(2, "已完善");

    private final Integer code;
    private final String desc;

    public static SetupStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
