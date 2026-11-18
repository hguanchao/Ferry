package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门禁规则。含 is_deleted，继承 BaseEntity 走逻辑删除。
 */
@Data
@TableName(value = "dev_gate_rule")
@EqualsAndHashCode(callSuper = true)
public class GateRule extends BaseEntity implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 1 组织 2 应用 3 分支 */
    private Integer scopeType;

    /** 应用id，应用/分支级必填 */
    private Long appId;

    /** 分支通配，仅分支级 */
    private String branchPattern;

    /** 1 单测通过率 2 行覆盖率 3 分支覆盖率 4 阻断漏洞 5 高危依赖 6 许可证黑名单 */
    private Integer ruleType;

    /** 规则名 */
    private String ruleName;

    /** 阈值 */
    private String threshold;

    /** 1 不低于 2 不高于 */
    private Integer compareDirection;

    /** 作用阶段，逗号分隔 */
    private String stageTypes;

    /** 是否允许豁免 */
    private Integer isExemptAllowed;

    /** 启停 */
    private Integer isEnabled;

    /** 规则版本，每次修改 +1 */
    private Integer version;
}
