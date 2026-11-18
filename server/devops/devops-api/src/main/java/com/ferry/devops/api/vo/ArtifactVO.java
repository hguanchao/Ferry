package com.ferry.devops.api.vo;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 制品列表项。
 */
@Data
public class ArtifactVO {

    @Schema(description = "制品id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "制品形态 1 包制品 2 镜像制品")
    private Integer form;

    @Schema(description = "sha256 摘要")
    private String digest;

    @Schema(description = "字节数")
    private Long size;

    @Schema(description = "产出运行")
    private Long runId;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "提交")
    private String commitId;

    @Schema(description = "构建号")
    private Long buildNo;

    @Schema(description = "构建镜像")
    private String buildImage;

    @Schema(description = "构建人")
    private Long builder;

    @Schema(description = "门禁汇总结论")
    private Integer gateConclusion;

    @Schema(description = "生产使用状态 1 当前生效 2 历史版本 3 从未使用")
    private Integer prodUseStatus;

    @Schema(description = "确认状态 1 正常 2 待确认")
    private Integer confirmStatus;

    @Schema(description = "所处环境类别列表 1测试 2预发 3生产")
    private List<Integer> envTags;

    @Schema(description = "删除序号，0 表示未删")
    private Long deleteSeq;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
