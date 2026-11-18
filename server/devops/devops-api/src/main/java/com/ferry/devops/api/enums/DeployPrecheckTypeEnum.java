package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 部署前校验项类型
 */
@Getter
@AllArgsConstructor
public enum DeployPrecheckTypeEnum {
    ARTIFACT_EXISTS(1, "制品存在"),
    FORM_MATCH(2, "形态匹配"),
    ENV_IDLE(3, "环境空闲"),
    DEPENDENCY_COMPATIBLE(4, "依赖兼容"),
    CHANGE_TICKET(5, "变更单");

    private final Integer code;
    private final String desc;

    public static DeployPrecheckTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
