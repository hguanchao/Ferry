package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 运行阶段状态
 */
@Getter
@AllArgsConstructor
public enum RunStageStatusEnum {
    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    SKIPPED("SKIPPED", "已复用"),
    BLOCKED("BLOCKED", "阻塞");

    private final String code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 阶段状态取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static RunStageStatusEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的阶段状态
     *
     * @param code 阶段状态取值
     * @return 是否已定义
     */
    public static boolean contains(String code) {
        return of(code) != null;
    }
}
