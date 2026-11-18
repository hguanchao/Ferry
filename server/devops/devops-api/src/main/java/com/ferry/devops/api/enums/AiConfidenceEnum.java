package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI 置信度
 */
@Getter
@AllArgsConstructor
public enum AiConfidenceEnum {
    HIGH(1, "高"),
    MEDIUM(2, "中"),
    LOW(3, "低");

    private final Integer code;
    private final String desc;

    public static AiConfidenceEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
