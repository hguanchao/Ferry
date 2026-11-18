package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 部署前校验结果（含启动令牌）
 */
@Data
public class PrecheckTokenVO {

    @Schema(description = "校验令牌，签名串含时间戳，5 分钟有效，供 deploy/start 使用")
    private String token;

    @Schema(description = "逐条校验结论")
    private List<DeployPrecheckVO> items;

    @Schema(description = "是否可部署")
    private Boolean canDeploy;
}
