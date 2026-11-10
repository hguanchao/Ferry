package com.ferry.system.api.enums;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MenuTypeEnum {
    /**
     * 目录
     */
    DIR(IntConst.ZERO, "目录"),
    /**
     * 菜单
     */
    MENU(IntConst.ONE, "菜单"),
    /**
     * 按钮
     */
    BUTTON(IntConst.TWO, "按钮");

    /**
     * 类型
     */
    private final Integer type;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 判断类型是否属于已定义取值
     *
     * @param type 类型
     * @return 是否已定义
     */
    public static boolean contains(Integer type) {
        return Arrays.stream(values()).anyMatch(item -> ObjUtil.equals(item.getType(), type));
    }
}
