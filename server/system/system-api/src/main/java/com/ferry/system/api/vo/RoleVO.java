package com.ferry.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RoleVO {

    @Schema(description = "角色id")
    private Long id;

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "数据范围 1：全部 2：自定义 3：本部门 4：本部门及以下 5：仅本人")
    private Integer dataScope;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "职责描述")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "自定义范围下勾选的部门id")
    private List<Long> deptIds;
}
