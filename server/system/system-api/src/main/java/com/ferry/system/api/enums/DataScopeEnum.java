package com.ferry.system.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 角色数据范围
 *
 * <p>取值与 sys_role.data_scope 一一对应，新增取值必须同步建表脚本与前端下拉，
 * 否则计算时会因找不到匹配项而按无权限处理。
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {
    /**
     * 全部数据
     */
    ALL(1, "全部数据"),
    /**
     * 自定义部门
     */
    CUSTOM(2, "自定义数据"),
    /**
     * 本部门
     */
    DEPT(3, "本部门数据"),
    /**
     * 本部门及以下
     */
    DEPT_AND_CHILD(4, "本部门及以下数据"),
    /**
     * 仅本人
     */
    SELF(5, "仅本人数据");

    /**
     * 取值
     */
    private final Integer code;

    /**
     * 描述
     */
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
