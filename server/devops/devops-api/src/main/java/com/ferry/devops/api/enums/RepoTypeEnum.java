package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 代码托管平台
 */
@Getter
@AllArgsConstructor
public enum RepoTypeEnum {
    GITLAB("GITLAB", "GitLab"),
    GITHUB("GITHUB", "GitHub"),
    GITEE("GITEE", "Gitee");

    private final String code;

    private final String desc;

    /**
     * 按取值查找枚举
     *
     * @param code 平台取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static RepoTypeEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
