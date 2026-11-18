package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

/**
 * 回滚记录
 */
@Data
@TableName("dev_rollback_record")
@EqualsAndHashCode(callSuper = true)
public class RollbackRecord extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 关联部署 */
    private Long deployId;

    /** 环境id */
    private Long envId;

    /** 回滚目标制品 */
    private Long artifactId;

    /** 回滚前制品 */
    private Long currentArtifactId;

    /** 原因 */
    private String reason;

    /** 操作人 */
    private Long operator;

    /** 确认人 */
    private Long confirmer;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 耗时毫秒 */
    private Long duration;

    /** 结果 1成功 2失败 */
    private Integer result;
}
