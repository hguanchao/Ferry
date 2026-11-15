package com.ferry.devops.api.enums;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水线模板状态
 */
@Getter
@AllArgsConstructor
public enum PipelineTemplateStatusEnum {
    DISABLED(IntConst.ZERO, "停用"),
    ENABLED(IntConst.ONE, "启用");

    private final Integer code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 模板状态取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static PipelineTemplateStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的模板状态
     *
     * @param code 模板状态取值
     * @return 是否已定义
     */
    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
