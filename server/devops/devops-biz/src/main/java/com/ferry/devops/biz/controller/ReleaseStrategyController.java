package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.ReleaseStrategyDTO;
import com.ferry.devops.api.vo.ReleaseStrategyVO;
import com.ferry.devops.biz.service.IReleaseStrategyService;
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
 * 发布策略控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/releaseStrategy")
@Tag(name = "发布策略", description = "发布窗口、封网排除与观察期配置")
public class ReleaseStrategyController {

    private final IReleaseStrategyService service;

    @GetMapping("/detail")
    @PreAuthorize("devops:releaseStrategy:detail")
    @Operation(summary = "发布策略详情")
    public R<ReleaseStrategyVO> detail(@RequestParam("appId") Long appId) {
        return R.data(service.detail(appId));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:releaseStrategy:save")
    @Operation(summary = "保存发布策略")
    public R<String> save(@RequestBody @Valid ReleaseStrategyDTO body) {
        return service.save(body) ? R.success("保存成功") : R.fail("保存失败");
    }
}
