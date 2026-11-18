package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 封网期
 */
@Data
@TableName("dev_network_closure")
public class NetworkClosure implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 开始日期 */
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date endDate;

    /** 范围 1全局 2应用 3团队 */
    private Integer scope;

    /** 应用id */
    private Long appId;

    /** 团队id */
    private Long teamId;

    /** 原因 */
    private String reason;
}
