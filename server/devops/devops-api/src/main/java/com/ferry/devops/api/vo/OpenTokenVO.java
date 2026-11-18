package com.ferry.devops.api.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开放 API Token VO
 */
@Data
public class OpenTokenVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "绑定应用")
    private Long appId;

    @Schema(description = "Token 名称")
    private String name;

    @Schema(description = "Token 掩码")
    private String tokenMask;

    @Schema(description = "每分钟配额")
    private Integer quotaPerMinute;

    @Schema(description = "1 正常 2 禁用")
    private Integer status;

    @Schema(description = "过期时间")
    private Date expireAt;

    @Schema(description = "最近调用")
    private Date lastUsedTime;

    @Schema(description = "最近轮换")
    private Date lastRotateTime;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "明文Token(仅签发/轮换时返回一次)")
    private String tokenPlain;
}
