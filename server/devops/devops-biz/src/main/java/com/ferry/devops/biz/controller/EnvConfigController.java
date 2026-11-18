package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.EnvConfigDTO;
import com.ferry.devops.api.vo.EnvConfigVO;
import com.ferry.devops.biz.service.IEnvConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/envConfig")
@Tag(name = "环境配置", description = "环境配置API接口控制器")
public class EnvConfigController {
    private final IEnvConfigService service;

    @GetMapping("/list")
    @PreAuthorize("devops:envConfig:pageList")
    @Operation(summary = "环境配置列表", parameters = @Parameter(name = "envId", description = "环境id", required = true))
    public R<List<EnvConfigVO>> list(@RequestParam("envId") Long envId) {
        return R.data(service.listByEnv(envId));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:envConfig:save")
    @Operation(summary = "环境配置新增或编辑")
    public R<String> save(@RequestBody @Valid EnvConfigDTO body) {
        return service.save(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @GetMapping("/diff")
    @PreAuthorize("devops:envConfig:detail")
    @Operation(summary = "环境配置差异", parameters = @Parameter(name = "envId", description = "环境id", required = true))
    public R<List<Map<String, Object>>> diff(@RequestParam("envId") Long envId) {
        return R.data(service.diff(envId));
    }
}
