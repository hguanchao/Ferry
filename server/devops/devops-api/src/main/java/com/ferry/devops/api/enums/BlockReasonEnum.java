package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 运行阻塞原因
 */
@Getter
@AllArgsConstructor
public enum BlockReasonEnum {
    GATE_FAILED(1, "门禁不通过"),
    GATE_FETCH_FAILED(2, "门禁数据获取失败"),
    MANUAL_PAUSED(3, "人工暂停");

    private final Integer code;
    private final String desc;

    public static BlockReasonEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
