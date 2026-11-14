package com.ferry.common.core.enums;

import com.ferry.common.core.constant.IntConst;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderEnum {
    MAN(IntConst.ONE, "男", "https://cdn4.iconfinder.com/data/icons/avatars-xmas-giveaway/128/man_male_avatar_portrait-128.png"),
    GIRL(IntConst.TWO, "女", "https://cdn4.iconfinder.com/data/icons/avatars-xmas-giveaway/128/girl_avatar_child_kid-128.png"),
    ;

    private final Integer gender;
    private final String describe;
    private final String avatar;

    /**
     * 根据性别获取默认头像
     *
     * @param gender 性别
     * @return 头像路径
     */
    public static String getAvatar(Integer gender) {
        for (GenderEnum en : values()) {
            if (en.gender.equals(gender)) {
                return en.avatar;
            }
        }
        return null;
    }
}
