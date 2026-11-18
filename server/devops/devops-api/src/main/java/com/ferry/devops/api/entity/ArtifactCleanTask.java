package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 制品清理任务。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_artifact_clean_task")
public class ArtifactCleanTask implements Serializable {

    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 任务时间 */
    private Date taskTime;

    /** 待清理数 */
    private Integer pendingCount;

    /** 实际清理数 */
    private Integer deletedCount;

    /** 1 待确认 2 已执行 3 失败 */
    private Integer status;

    /** 清单文件地址 */
    private String manifestFile;

    /** 执行人 */
    private Long operator;
}
