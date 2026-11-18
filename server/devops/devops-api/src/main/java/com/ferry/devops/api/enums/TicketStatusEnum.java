package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 变更单状态
 */
@Getter
@AllArgsConstructor
public enum TicketStatusEnum {
    DRAFT(1, "草稿"),
    PENDING_APPROVAL(2, "待审批"),
    APPROVED(3, "已批准"),
    IMPLEMENTED(4, "已实施"),
    CLOSED(5, "已关闭"),
    CANCELLED(6, "已取消");

    private final Integer code;
    private final String desc;

    public static TicketStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
