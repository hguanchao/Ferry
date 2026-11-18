package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 发布单状态
 */
@Getter
@AllArgsConstructor
public enum RcStatusEnum {
    DRAFT(1, "草稿"),
    PENDING_APPROVAL(2, "待审批"),
    APPROVED(3, "已批准"),
    RELEASING(4, "发布中"),
    OBSERVING(5, "观察中"),
    COMPLETED(6, "已完成"),
    ROLLED_BACK(7, "已回滚"),
    REJECTED(8, "已驳回"),
    CANCELLED(9, "已取消");

    private final Integer code;
    private final String desc;

    public static RcStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
