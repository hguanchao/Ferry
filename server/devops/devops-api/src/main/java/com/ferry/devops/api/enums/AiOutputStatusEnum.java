package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI 输出状态
 */
@Getter
@AllArgsConstructor
public enum AiOutputStatusEnum {
    SUCCESS(1, "成功"),
    DISCARDED(2, "丢弃"),
    TIMEOUT(3, "超时"),
    NO_EVIDENCE(4, "无法归因");

    private final Integer code;
    private final String desc;

    public static AiOutputStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
