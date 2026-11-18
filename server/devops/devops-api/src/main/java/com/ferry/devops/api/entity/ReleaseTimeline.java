package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 发布单时间线
 */
@Data
@TableName("dev_release_timeline")
public class ReleaseTimeline implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 发布单id */
    private Long rcId;

    /** 节点 */
    private String node;

    /** 操作人 */
    private Long operator;

    /** 备注 */
    private String remark;

    /** 时间 */
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
