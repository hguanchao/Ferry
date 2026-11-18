package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 门禁规则类型（6 维度）。比率类 1-3，数量类 4-5，黑名单类 6。
 */
@Getter
@AllArgsConstructor
public enum RuleTypeEnum {
    UNIT_TEST_PASS_RATE(1, "单元测试通过率", true),
    LINE_COVERAGE(2, "行覆盖率", true),
    BRANCH_COVERAGE(3, "分支覆盖率", true),
    BLOCKING_VULNERABILITY(4, "阻断级漏洞数", false),
    HIGH_RISK_DEPENDENCY(5, "高危依赖漏洞数", false),
    LICENSE_BLACKLIST(6, "许可证黑名单", false);

    private final Integer code;
    private final String desc;
    /** 是否比率类（0-100），否则数量类/黑名单类 */
    private final boolean ratio;

    public static RuleTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
