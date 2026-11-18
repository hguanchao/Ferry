package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 代码平台
 */
@Getter
@AllArgsConstructor
public enum RepoTypeEnum {
    GITLAB(1, "GitLab"),
    GITHUB(2, "GitHub"),
    GITEE(3, "Gitee");

    private final Integer code;
    private final String desc;

    public static RepoTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
