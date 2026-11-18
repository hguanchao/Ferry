package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 门禁结果。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_gate_result")
public class GateResult implements Serializable {

    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 阶段id */
    private Long stageId;

    /** 规则id */
    private Long ruleId;

    /** 规则快照 */
    private String ruleSnapshot;

    /** 当前值 */
    private String currentValue;

    /** 阈值 */
    private String threshold;

    /** 1 通过 2 不通过 3 不适用 4 已豁免 5 数据获取失败 */
    private Integer conclusion;

    /** 证据链接 */
    private String evidence;

    /** 报告来源 */
    private String source;

    /** schema 版本 */
    private String schemaVersion;

    /** 获取失败原因 */
    private String fetchError;

    /** 判定时间 */
    private Date createTime;
}
