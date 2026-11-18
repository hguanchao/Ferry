package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布单可选制品
 */
@Data
public class ArtifactCandidateVO {

    @Schema(description = "制品id")
    private Long artifactId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "摘要")
    private String digest;

    @Schema(description = "形态")
    private Integer form;

    @Schema(description = "门禁结论")
    private Integer gateConclusion;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
