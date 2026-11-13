package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 流水线定义
 *
 * <p>主表只保存流水线本身与当前生效版本的指针，定义内容按版本存放在快照表；
 * 列表展示的阶段数、版本号在这里冗余，避免列表解析快照。
 */
@Data
@TableName(value = "dev_pipeline")
@EqualsAndHashCode(callSuper = true)
public class Pipeline extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 所属应用id
     */
    private Long appId;

    /**
     * 流水线名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 当前生效版本id
     */
    private Long currentVersionId;

    /**
     * 当前生效版本号
     */
    private Integer currentVersionNo;

    /**
     * 当前版本阶段数
     */
    private Integer stageCount;
}
