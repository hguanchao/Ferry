package com.ferry.devops.api.enums;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水线模板层级
 */
@Getter
@AllArgsConstructor
public enum PipelineTemplateScopeEnum {
    ORG(IntConst.ZERO, "组织级"),
    TEAM(IntConst.ONE, "团队级");

    private final Integer code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 模板层级取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static PipelineTemplateScopeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的模板层级
     *
     * @param code 模板层级取值
     * @return 是否已定义
     */
    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
