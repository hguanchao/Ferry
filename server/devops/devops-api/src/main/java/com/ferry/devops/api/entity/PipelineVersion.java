package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 流水线版本快照
 *
 * <p>版本一经生成只读不改，运行实例将来按版本 id 绑定快照执行；
 * 因此定义内容整体存 JSON，不在版本表里拆阶段和任务。
 */
@Data
@TableName(value = "dev_pipeline_version")
@EqualsAndHashCode(callSuper = true)
public class PipelineVersion extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 所属流水线id
     */
    private Long pipelineId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 定义快照 JSON
     */
    private String content;

    /**
     * 定义内容 SHA-256
     */
    private String contentHash;

    /**
     * 变更说明
     */
    private String changeNote;

    /**
     * 变更人名称快照
     */
    private String createUserName;
}
