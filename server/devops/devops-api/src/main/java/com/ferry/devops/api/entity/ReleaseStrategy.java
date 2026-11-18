package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 发布策略
 */
@Data
@TableName("dev_release_strategy")
public class ReleaseStrategy implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 应用id */
    private Long appId;

    /** 多段发布窗口 */
    private String windowsJson;

    /** 是否排除封网 */
    private Integer isClosureExcluded;

    /** 默认观察期覆盖值 */
    private Integer observeMinutes;
}
