package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 阶段状态
 */
@Getter
@AllArgsConstructor
public enum StageStatusEnum {
    NOT_STARTED(1, "未开始"),
    RUNNING(2, "进行中"),
    SUCCESS(3, "成功"),
    FAILURE(4, "失败"),
    BLOCKED(5, "阻塞"),
    SKIPPED(6, "跳过"),
    TIMEOUT(7, "超时"),
    REUSED(8, "已复用"),
    CANCELLED(9, "已取消");

    private final Integer code;
    private final String desc;

    public static StageStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
