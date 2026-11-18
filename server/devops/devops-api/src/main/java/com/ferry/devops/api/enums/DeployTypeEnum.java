package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 部署类型
 */
@Getter
@AllArgsConstructor
public enum DeployTypeEnum {
    SSH(1, "SSH"),
    DOCKER(2, "Docker"),
    KUBERNETES(3, "Kubernetes");

    private final Integer code;
    private final String desc;

    public static DeployTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
