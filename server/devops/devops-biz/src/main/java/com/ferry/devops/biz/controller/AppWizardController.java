package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.WizardProgressVO;
import com.ferry.devops.biz.service.IAppWizardService;
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
 * 应用接入向导与详情聚合接口
 */
@RestController
@AllArgsConstructor
@RequestMapping("/app")
@Tag(name = "应用接入向导", description = "接入向导与应用详情聚合")
public class AppWizardController {
    private final IAppWizardService service;

    @PostMapping("/wizard/saveStep")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "接入向导保存步骤")
    public R<String> saveWizardStep(@RequestBody @Valid WizardStepDTO body) {
        return service.saveStep(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @GetMapping("/wizard/progress")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "接入向导进度")
    public R<WizardProgressVO> wizardProgress(@RequestParam("id") Long id) {
        return R.data(service.progress(id));
    }

    @GetMapping("/aggregate/overview")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "应用概览")
    public R<AppOverviewVO> overview(@RequestParam("id") Long id) {
        return R.data(service.overview(id));
    }
}
