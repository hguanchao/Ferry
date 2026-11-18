package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导出任务
 */
@Data
@TableName("sys_export_task")
@EqualsAndHashCode(callSuper = true)
public class ExportTask extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 模块 */
    private String module;

    /** 筛选条件 */
    private String filters;

    /** 勾选id */
    private String ids;

    /** 1 CSV 2 Excel */
    private Integer format;

    /** 行数 */
    private Integer rowCount;

    /** 1 待执行 2 执行中 3 成功 4 失败 5 已过期 */
    private Integer status;

    /** 文件地址 */
    private String fileUrl;

    /** 失败原因 */
    private String failReason;

    /** 重试 */
    private Integer retryCount;

    /** 过期时间 */
    private Date expireAt;
}
