package com.ferry.devops.biz.excel;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 流水线导出行
 *
 * <p>列顺序即 Excel 列顺序，与列表展示顺序保持一致：增删列只改这个类和
 * {@code PipelineServiceImpl#buildExportRows}，不再另维护一份表头映射。
 * 字段一律按字符串写，发布版本号、时间这类内容交给数值类型会被单元格格式改写。
 */
@Data
@ExcelIgnoreUnannotated
public class PipelineExportRow {

    @ExcelProperty("流水线ID")
    @ColumnWidth(20)
    private String id;

    @ExcelProperty("应用编码")
    @ColumnWidth(18)
    private String appCode;

    @ExcelProperty("应用名称")
    @ColumnWidth(20)
    private String appName;

    @ExcelProperty("流水线名称")
    @ColumnWidth(30)
    private String name;

    @ExcelProperty("当前版本")
    @ColumnWidth(12)
    private String currentVersion;

    @ExcelProperty("阶段数")
    @ColumnWidth(10)
    private String stageCount;

    @ExcelProperty("默认分支")
    @ColumnWidth(16)
    private String defaultBranch;

    @ExcelProperty("负责人")
    @ColumnWidth(22)
    private String ownerNames;

    @ExcelProperty("描述")
    @ColumnWidth(40)
    private String description;

    @ExcelProperty("更新时间")
    @ColumnWidth(20)
    private String updateTime;
}
