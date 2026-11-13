package com.ferry.devops.api.dto;

import com.ferry.devops.api.dto.query.PipelineQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线导出条件
 *
 * <p>继承列表查询条件，保证「导出的数据与列表一致」是靠同一份条件对象，
 * 而不是两边各维护一套字段名。勾选项单独放在 ids：两者同时存在时表示
 * 「在勾选范围内按当前筛选导出」，由 SQL 一起生效。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineExportDTO extends PipelineQuery {

    @Schema(description = "勾选的流水线id，为空表示导出当前筛选结果")
    private List<Long> ids;
}
