package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 运行阶段门禁结果。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_run_stage_gate")
public class RunStageGate implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 运行阶段id */
    private Long runStageId;

    /** 规则id */
    private Long ruleId;

    /** 规则类型 1=单元测试通过率 2=行覆盖率 3=分支覆盖率 4=阻断级漏洞数 5=高危依赖漏洞数 6=许可证黑名单 */
    private Integer ruleType;

    /** 规则版本号 */
    private Integer ruleVersion;

    /** 规则快照(JSON) */
    private String ruleSnapshot;

    /** 当前值 */
    private String currentValue;

    /** 阈值 */
    private String threshold;

    /** 状态 1=通过 2=不通过 3=不适用 4=已豁免 5=数据获取失败 */
    private Integer status;

    /** 证据链接 */
    private String evidence;

    /** 报告来源 */
    private String source;

    /** 报告schema版本 */
    private String schemaVersion;

    /** 原因说明 数据获取失败/不适用/豁免原因 */
    private String fetchError;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
