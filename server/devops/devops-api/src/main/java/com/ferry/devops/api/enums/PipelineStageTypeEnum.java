package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水线阶段类型
 *
 * <p>阶段顺序由定义里的数组顺序决定，类型只表达语义；取值固定，前端与后端共用同一套编码。
 */
@Getter
@AllArgsConstructor
public enum PipelineStageTypeEnum {
    /**
     * 构建
     */
    BUILD("BUILD", "构建"),
    /**
     * 单元测试
     */
    UNIT_TEST("UNIT_TEST", "单元测试"),
    /**
     * 静态扫描
     */
    STATIC_SCAN("STATIC_SCAN", "静态扫描"),
    /**
     * 制品入库
     */
    ARTIFACT_PUSH("ARTIFACT_PUSH", "制品入库"),
    /**
     * 部署非生产
     */
    DEPLOY_NON_PROD("DEPLOY_NON_PROD", "部署非生产"),
    /**
     * 集成测试
     */
    INTEGRATION_TEST("INTEGRATION_TEST", "集成测试"),
    /**
     * 准入
     */
    ADMISSION("ADMISSION", "准入"),
    /**
     * 生产发布
     */
    PROD_RELEASE("PROD_RELEASE", "生产发布");

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
     * @param code 阶段类型取值
     * @return 对应枚举，取值未定义时返回 null
     */
    public static PipelineStageTypeEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断取值是否为已定义的阶段类型
     *
     * @param code 阶段类型取值
     * @return 是否已定义
     */
    public static boolean contains(String code) {
        return of(code) != null;
    }
}
