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
 * 部署批次
 */
@Data
@TableName("dev_deploy_batch")
@EqualsAndHashCode(callSuper = true)
public class DeployBatch extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 部署id */
    private Long deployId;

    /** 批次号 */
    private Integer batchNo;

    /** 影响实例数 */
    private Integer targetCount;

    /** 实例清单快照 */
    private String targetSnapshot;

    /** 状态 1-7 */
    private Integer status;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 健康结论 1通过 2不通过 */
    private Integer healthResult;
}
