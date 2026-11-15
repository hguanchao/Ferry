package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 流水线模板版本快照
 */
@Data
@TableName(value = "dev_pipeline_template_version")
@EqualsAndHashCode(callSuper = true)
public class PipelineTemplateVersion extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 所属模板id
     */
    private Long templateId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 定义快照 JSON，含阶段与占位变量
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
