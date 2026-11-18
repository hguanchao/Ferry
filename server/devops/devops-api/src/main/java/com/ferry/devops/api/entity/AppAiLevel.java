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
 * 应用 AI 授权
 */
@Data
@TableName("dev_app_ai_level")
@EqualsAndHashCode(callSuper = true)
public class AppAiLevel extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id */
    private Long appId;

    /** 已开通最高级别 */
    private Integer level;

    /** 授权人 */
    private Long grantedBy;

    /** 授权时间 */
    private Date grantedAt;
}
