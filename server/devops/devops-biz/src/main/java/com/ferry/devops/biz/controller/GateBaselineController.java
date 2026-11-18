package com.ferry.devops.biz.controller;

import java.util.Map;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.GateBaselineResetDTO;
import com.ferry.devops.api.vo.BaselineVO;
import com.ferry.devops.biz.service.IGateBaselineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存量基线。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/gateBaseline")
@Tag(name = "存量基线", description = "基线快照、重设与审批")
public class GateBaselineController {

    private final IGateBaselineService service;

    @GetMapping("/detail")
    @PreAuthorize("devops:baseline:detail")
    @Operation(summary = "存量基线-详情")
    public R<BaselineVO> detail(@RequestParam("appId") Long appId) {
        return R.data(service.detail(appId));
    }

    @PostMapping("/reset")
    @PreAuthorize("devops:baseline:reset")
    @Operation(summary = "存量基线-重设申请")
    public R<String> reset(@RequestBody @Valid GateBaselineResetDTO body) {
        return service.reset(body) ? R.success("申请已提交") : R.fail("申请失败");
    }

    @PostMapping("/approve")
    @PreAuthorize("devops:baseline:approve")
    @Operation(summary = "存量基线-审批")
    public R<String> approve(@RequestBody Map<String, Object> body) {
        Long applyId = Long.valueOf(String.valueOf(body.get("applyId")));
        Boolean pass = Boolean.valueOf(String.valueOf(body.get("pass")));
        String opinion = body.get("opinion") == null ? null : String.valueOf(body.get("opinion"));
        return service.approve(applyId, pass, opinion) ? R.success("审批完成") : R.fail("审批失败");
    }
}
