package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 准入检查项
 */
@Data
@TableName("dev_release_check_item")
public class ReleaseCheckItem implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 发布单id */
    private Long rcId;

    /** 检查项 1门禁 2影响面 3回滚方案 4监控覆盖 5变更单 6值班确认 */
    private Integer checkType;

    /** 系统结论 */
    private Integer systemConclusion;

    /** 人工结论 */
    private Integer manualConclusion;

    /** 备注 */
    private String remark;

    /** 证据 */
    private String evidence;
}
