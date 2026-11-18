package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 提示版本
 */
@Data
@TableName("dev_ai_prompt_version")
@EqualsAndHashCode(callSuper = true)
public class AiPromptVersion extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 版本号 */
    private String code;

    /** 提示内容 */
    private String content;

    /** 1 草稿 2 生效 3 归档 */
    private Integer status;
}
