package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用选项
 */
@Data
public class AppOptionVO {

    @Schema(description = "应用id")
    private Long value;

    @Schema(description = "应用名称")
    private String label;

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "仓库git地址")
    private String repoUrl;

    @Schema(description = "应用默认分支")
    private String repoBranch;

    @Schema(description = "应用登记提交（7–40 位十六进制）")
    private String repoCommitId;
}