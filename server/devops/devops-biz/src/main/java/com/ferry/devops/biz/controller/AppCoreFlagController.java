package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AppCoreFlagApplyDTO;
import com.ferry.devops.api.dto.AppCoreFlagApproveDTO;
import com.ferry.devops.biz.service.IAppCoreFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 核心链路变更接口
 */
@RestController
@AllArgsConstructor
@RequestMapping("/app/coreFlag")
@Tag(name = "核心链路变更", description = "核心链路变更申请与审批")
public class AppCoreFlagController {
    private final IAppCoreFlagService service;

    @PostMapping("/apply")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "核心链路变更申请")
    public R<String> apply(@RequestBody @Valid AppCoreFlagApplyDTO body) {
        return service.apply(body) ? R.success("申请已提交") : R.fail("申请失败");
    }

    @PostMapping("/approve")
    @PreAuthorize("devops:app:coreFlagApprove")
    @Operation(summary = "核心链路变更审批")
    public R<String> approve(@RequestBody @Valid AppCoreFlagApproveDTO body) {
        return service.approve(body) ? R.success("审批完成") : R.fail("审批失败");
    }
}
