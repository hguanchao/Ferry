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
 * 发布记录
 */
@Data
@TableName("dev_release_record")
@EqualsAndHashCode(callSuper = true)
public class ReleaseRecord extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 发布单id */
    private Long rcId;

    /** 应用id */
    private Long appId;

    /** 环境id */
    private Long envId;

    /** 部署类型 */
    private Integer deployType;

    /** 制品id */
    private Long artifactId;

    /** 批次明细快照 */
    private String batchDetail;

    /** 健康检查快照 */
    private String healthSnapshot;

    /** 结果 1成功 2失败 3已回滚 4已取消 */
    private Integer result;

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

    /** 是否回滚过 */
    private Integer isRolledBack;
}
