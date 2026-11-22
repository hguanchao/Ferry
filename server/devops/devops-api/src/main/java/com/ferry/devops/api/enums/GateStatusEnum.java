package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 运行阶段门禁状态
 */
@Getter
@AllArgsConstructor
public enum GateStatusEnum {
    PASS(1, "通过"),
    FAIL(2, "不通过"),
    NOT_APPLICABLE(3, "不适用"),
    EXEMPTED(4, "已豁免"),
    FETCH_FAILED(5, "数据获取失败");

    private final Integer code;
    private final String desc;

    public static GateStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
