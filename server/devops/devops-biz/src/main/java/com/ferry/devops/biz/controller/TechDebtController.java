package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.FalsePositiveApplyDTO;
import com.ferry.devops.api.dto.TechDebtAssignDTO;
import com.ferry.devops.api.dto.query.TechDebtQuery;
import com.ferry.devops.api.vo.TechDebtVO;
import com.ferry.devops.biz.service.ITechDebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 技术债看板。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/techDebt")
@Tag(name = "技术债看板", description = "存量问题清单、指派、误报与导出")
public class TechDebtController {

    private final ITechDebtService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:techDebt:pageList")
    @Operation(summary = "技术债-分页列表")
    public R<IPage<TechDebtVO>> pageList(@RequestBody TechDebtQuery query) {
        return R.data(service.pageList(query));
    }

    @PostMapping("/assign")
    @PreAuthorize("devops:techDebt:assign")
    @Operation(summary = "技术债-指派")
    public R<String> assign(@RequestBody @Valid TechDebtAssignDTO body) {
        return service.assign(body) ? R.success("指派成功") : R.fail("指派失败");
    }

    @PostMapping("/falsePositive/apply")
    @PreAuthorize("devops:techDebt:falsePositive")
    @Operation(summary = "技术债-误报申请")
    public R<String> falsePositive(@RequestBody @Valid FalsePositiveApplyDTO body) {
        return service.applyFalsePositive(body) ? R.success("已标记误报") : R.fail("操作失败");
    }

    @PostMapping("/export")
    @PreAuthorize("devops:techDebt:pageList")
    @Operation(summary = "技术债-导出")
    public R<String> export(@RequestBody TechDebtQuery query) {
        return R.data(service.export(query));
    }
}
