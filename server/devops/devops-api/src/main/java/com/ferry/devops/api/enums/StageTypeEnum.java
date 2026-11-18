package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 阶段类型
 */
@Getter
@AllArgsConstructor
public enum StageTypeEnum {
    BUILD(1, "构建"),
    UNIT_TEST(2, "单元测试"),
    STATIC_SCAN(3, "静态扫描"),
    ARTIFACT(4, "制品入库"),
    DEPLOY_NON_PROD(5, "部署非生产"),
    INTEGRATION_TEST(6, "集成测试"),
    ADMISSION(7, "准入"),
    PROD_RELEASE(8, "生产发布");

    private final Integer code;
    private final String desc;

    public static StageTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }
}
