package com.ferry.devops.biz.gate;

import java.util.List;

/**
 * 阶段门禁报告契约。
 *
 * <p>单测/静态扫描任务在 Jenkins 工作区产出固定路径的归一化 JSON，由 Ferry 拉取归档后判定。
 */
public final class GateReportContract {

    /** 单测归一化报告路径 */
    public static final String UNIT_TEST_REPORT_PATH = "reports/ferry-unit-test-summary.json";

    /** 静态扫描归一化报告路径 */
    public static final String SCAN_REPORT_PATH = "reports/ferry-gate-summary.json";

    /** 报告 schema 版本 */
    public static final String SCHEMA_VERSION = "1.0";

    /** 单测阶段适用规则类型 */
    public static final List<Integer> UNIT_TEST_RULE_TYPES = List.of(1, 2, 3);

    /** 静态扫描阶段适用规则类型 */
    public static final List<Integer> SCAN_RULE_TYPES = List.of(4, 5, 6);

    private GateReportContract() {
    }
}
