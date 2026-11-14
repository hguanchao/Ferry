package com.ferry.devops.api.vo.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 流水线导出行
 */
@Data
public class PipelineExportRow {

    @ExcelProperty("流水线ID")
    private String id;

    @ExcelProperty("应用编码")
    private String appCode;

    @ExcelProperty("应用名称")
    private String appName;

    @ExcelProperty("流水线名称")
    private String name;

    @ExcelProperty("当前版本")
    private String currentVersion;

    @ExcelProperty("阶段数")
    private String stageCount;

    @ExcelProperty("默认分支")
    private String defaultBranch;

    @ExcelProperty("负责人")
    private String ownerNames;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("更新时间")
    private String updateTime;
}
