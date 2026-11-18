package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 变更单类型
 */
@Getter
@AllArgsConstructor
public enum ChangeTicketTypeEnum {
    NORMAL(1, "常规"),
    EMERGENCY(2, "紧急"),
    STANDARD(3, "标准");

    private final Integer code;
    private final String desc;

    public static ChangeTicketTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
