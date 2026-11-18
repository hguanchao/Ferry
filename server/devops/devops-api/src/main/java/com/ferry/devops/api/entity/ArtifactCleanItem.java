package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 制品清理明细。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_artifact_clean_item")
public class ArtifactCleanItem implements Serializable {

    @TableId
    private Long id;

    /** 任务id */
    private Long taskId;

    /** 制品id */
    private Long artifactId;

    /** 版本快照 */
    private String version;

    /** 摘要快照 */
    private String digest;

    /** 大小 */
    private Long size;

    /** 保留原因 */
    private String keepReason;
}
