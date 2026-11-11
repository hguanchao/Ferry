package com.ferry.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class DictVO {

    @Schema(description = "字典id")
    private Long id;

    @Schema(description = "父字典id")
    private Long parentId;

    @Schema(description = "字典码")
    private String code;

    @Schema(description = "字典标签")
    private String label;

    @Schema(description = "字典值")
    private String value;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
