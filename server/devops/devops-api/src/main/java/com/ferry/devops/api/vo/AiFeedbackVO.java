package com.ferry.devops.api.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 反馈 VO
 */
@Data
public class AiFeedbackVO {

    @Schema(description = "反馈id")
    private Long id;

    @Schema(description = "输出id")
    private Long outputId;

    @Schema(description = "是否有帮助")
    private Integer isHelpful;

    @Schema(description = "正确答案")
    private String correctAnswer;

    @Schema(description = "反馈人")
    private Long userId;

    @Schema(description = "反馈时间")
    private Date createTime;
}
