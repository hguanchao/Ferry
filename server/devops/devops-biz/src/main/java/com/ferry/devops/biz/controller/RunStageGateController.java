package com.ferry.devops.biz.controller;

import java.util.List;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.GateExemptionDTO;
import com.ferry.devops.api.vo.RunStageGateVO;
import com.ferry.devops.biz.service.IGateExemptionService;
import com.ferry.devops.biz.service.IRunStageGateService;
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
 * 运行阶段门禁结果展示与豁免申请。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/runStageGate")
@Tag(name = "运行阶段门禁", description = "运行阶段门禁结果展示与单条豁免")
public class RunStageGateController {

    private final IRunStageGateService runStageGateService;
    private final IGateExemptionService gateExemptionService;

    @GetMapping("/list")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行阶段门禁结果-运行列表")
    public R<List<RunStageGateVO>> list(@RequestParam("runId") Long runId) {
        return R.data(runStageGateService.listByRun(runId));
    }

    @PostMapping("/exempt")
    @PreAuthorize("devops:gateExemption:apply")
    @Operation(summary = "运行阶段门禁结果-申请豁免")
    public R<String> exempt(@RequestBody @Valid GateExemptionDTO body) {
        return gateExemptionService.exempt(body) ? R.success("豁免成功") : R.fail("豁免失败");
    }
}
