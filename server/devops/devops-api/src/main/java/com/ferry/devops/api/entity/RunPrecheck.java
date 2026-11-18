package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 触发预校验
 */
@Data
@TableName(value = "dev_run_precheck")
public class RunPrecheck implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 1仓库可达 2分支存在 3提交可达 4定义匹配 5幂等检查 */
    private Integer itemType;

    /** 1通过 2不通过 3合并 4复用 */
    private Integer conclusion;

    /** 明细 */
    private String detail;

    /** 校验时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date checkedAt;
}
