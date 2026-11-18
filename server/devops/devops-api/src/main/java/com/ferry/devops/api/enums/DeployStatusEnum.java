package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 部署状态
 */
@Getter
@AllArgsConstructor
public enum DeployStatusEnum {
    NOT_STARTED(1, "未开始"),
    RUNNING(2, "执行中"),
    OBSERVING(3, "观察中"),
    SUCCESS(4, "成功"),
    FAILURE(5, "失败"),
    ROLLED_BACK(6, "已回滚"),
    CANCELLED(7, "已取消");

    private final Integer code;
    private final String desc;

    public static DeployStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
