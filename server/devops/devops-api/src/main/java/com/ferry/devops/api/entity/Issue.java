package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 存量问题清单（技术债）。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_issue")
public class Issue implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id */
    private Long appId;

    /** 指纹 */
    private String fingerprint;

    /** 级别 1阻断 2高危 3中危 4低危 */
    private Integer severity;

    /** 规则类型 */
    private Integer ruleType;

    /** 文件路径 */
    private String filePath;

    /** 1 新增 2 遗留 3 已修复 4 误报 */
    private Integer status;

    /** 修复责任人 */
    private Long assignee;

    /** 首次出现 */
    private Date firstSeen;

    /** 最近出现 */
    private Date lastSeen;
}
