package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 部署前校验
 */
@Data
@TableName("dev_deploy_precheck")
public class DeployPrecheck implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 部署id */
    private Long deployId;

    /** 校验项 1制品存在 2形态匹配 3环境空闲 4依赖兼容 5变更单 */
    private Integer checkType;

    /** 结论 1通过 2不通过 */
    private Integer conclusion;

    /** 明细 */
    private String detail;

    /** 校验时间 */
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date checkedAt;
}
