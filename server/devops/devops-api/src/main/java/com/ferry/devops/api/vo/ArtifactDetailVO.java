package com.ferry.devops.api.vo;
import java.util.Date;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 制品详情：元数据 + 环境标记 + 晋级/部署时间线。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArtifactDetailVO extends ArtifactVO {

    @Schema(description = "环境标记列表")
    private List<ArtifactEnvVO> envs;

    @Schema(description = "晋级时间线")
    private List<TimelineItem> promoteTimeline;

    @Schema(description = "部署时间线")
    private List<TimelineItem> deployTimeline;

    /**
     * 晋级/部署时间线条目。作为详情 VO 的内嵌类型，避免额外文件。
     */
    @Data
    public static class TimelineItem {

        @Schema(description = "环境id")
        private Long envId;

        @Schema(description = "环境类别")
        private Integer envCategory;

        @Schema(description = "操作人")
        private Long operator;

        @Schema(description = "时间")
        private java.util.Date time;

        @Schema(description = "结果/说明")
        private String result;
    }
}
