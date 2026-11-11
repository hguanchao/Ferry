package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 代码托管平台
 *
 * <p>取值即仓库地址所属平台，保存时要与仓库地址域名一致，否则运行期会拉错仓库。
 */
@Getter
@AllArgsConstructor
public enum RepoTypeEnum {
    /**
     * GitLab
     */
    GITLAB("GITLAB", "GitLab"),
    /**
     * GitHub
     */
    GITHUB("GITHUB", "GitHub"),
    /**
     * Gitee
     */
    GITEE("GITEE", "Gitee");

    /**
     * 取值
     */
    private final String code;

    /**
     * 描述
     */
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
