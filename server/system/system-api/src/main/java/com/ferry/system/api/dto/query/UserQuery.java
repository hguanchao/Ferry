package com.ferry.system.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends Query {

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "用户账号")
    private String account;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "账号状态 0：禁用 1：正常")
    private Integer status;

    @Schema(description = "部门id")
    private String deptId;
}