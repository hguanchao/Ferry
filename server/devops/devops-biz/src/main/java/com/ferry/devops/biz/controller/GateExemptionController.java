package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.query.GateExemptionQuery;
import com.ferry.devops.api.vo.GateExemptionVO;
import com.ferry.devops.biz.service.IGateResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门禁豁免列表。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/gateExemption")
@Tag(name = "门禁豁免", description = "豁免记录查询")
public class GateExemptionController {

    private final IGateResultService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:gateExemption:pageList")
    @Operation(summary = "门禁豁免-分页列表")
    public R<IPage<GateExemptionVO>> pageList(@RequestBody GateExemptionQuery query) {
        return R.data(service.exemptionList(query));
    }

    @PostMapping("/revoke/{id}")
    @PreAuthorize("devops:gateExemption:apply")
    @Operation(summary = "门禁豁免-撤销")
    public R<String> revoke(@PathVariable("id") Long id) {
        return service.revoke(id) ? R.success("撤销成功") : R.fail("撤销失败");
    }
}
