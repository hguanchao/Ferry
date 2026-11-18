package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 版本摘要（流水线与模板共用）
 */
@Data
public class VersionVO {

    @Schema(description = "版本id")
    private Long id;

    @Schema(description = "所属对象id（流水线或模板）")
    private Long bizId;

    @Schema(description = "流水线id（兼容前端契约）")
    private Long pipelineId;

    @Schema(description = "模板id")
    private Long templateId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "内容摘要")
    private String contentHash;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "回退来源版本")
    private Long sourceVersionId;

    @Schema(description = "1表单 2YAML 3回退 4模板合并")
    private Integer sourceType;

    @Schema(description = "内容 JSON")
    private String contentJson;

    @Schema(description = "内容 YAML")
    private String contentYaml;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "创建时间")
    private Date createTime;
}
