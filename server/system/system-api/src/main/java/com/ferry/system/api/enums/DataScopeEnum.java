package com.ferry.system.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 角色数据范围
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {
    ALL(1, "全部数据"),
    CUSTOM(2, "自定义数据"),
    DEPT(3, "本部门数据"),
    DEPT_AND_CHILD(4, "本部门及以下数据"),
    SELF(5, "仅本人数据");

    private final Integer code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 数据范围取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static DataScopeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
