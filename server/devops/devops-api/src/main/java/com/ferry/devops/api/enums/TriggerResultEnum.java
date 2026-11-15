package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 触发结果
 *
 * <p>PENDING_REUSE 仅用于触发接口的响应，表示命中成功运行、等待用户确认是否复用制品，不落库。
 */
@Getter
@AllArgsConstructor
public enum TriggerResultEnum {
    CREATED("CREATED", "已创建运行"),
    MERGED("MERGED", "已合并"),
    REUSED("REUSED", "复用运行"),
    REJECTED("REJECTED", "已拒绝"),
    PENDING_REUSE("PENDING_REUSE", "待确认复用");

    private final String code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 触发结果取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static TriggerResultEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的触发结果
     *
     * @param code 触发结果取值
     * @return 是否已定义
     */
    public static boolean contains(String code) {
        return of(code) != null;
    }
}
