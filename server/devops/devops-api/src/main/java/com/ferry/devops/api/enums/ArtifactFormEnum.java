package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 制品形态
 */
@Getter
@AllArgsConstructor
public enum ArtifactFormEnum {
    PACKAGE(1, "包制品"),
    IMAGE(2, "镜像制品");

    private final Integer code;
    private final String desc;

    public static ArtifactFormEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
