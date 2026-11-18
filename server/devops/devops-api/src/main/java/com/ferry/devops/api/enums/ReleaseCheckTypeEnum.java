package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 发布准入检查项类型
 */
@Getter
@AllArgsConstructor
public enum ReleaseCheckTypeEnum {
    GATE(1, "门禁"),
    IMPACT(2, "影响面"),
    ROLLBACK_PLAN(3, "回滚方案"),
    MONITOR_COVERAGE(4, "监控覆盖"),
    CHANGE_TICKET(5, "变更单"),
    ONCALL_CONFIRM(6, "值班确认");

    private final Integer code;
    private final String desc;

    public static ReleaseCheckTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
