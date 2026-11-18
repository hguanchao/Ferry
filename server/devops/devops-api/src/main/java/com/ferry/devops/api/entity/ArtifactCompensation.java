package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 制品补偿记录。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_artifact_compensation")
public class ArtifactCompensation implements Serializable {

    @TableId
    private Long id;

    /** 制品id */
    private Long artifactId;

    /** 运行id */
    private Long runId;

    /** 1 push 成功写表失败 2 写表成功 push 失败 */
    private Integer scene;

    /** 尝试次数 */
    private Integer attempt;

    /** 1 处理中 2 成功 3 失败 */
    private Integer status;

    /** 最近错误 */
    private String lastError;
}
