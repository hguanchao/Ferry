package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AppDependencyDTO;
import com.ferry.devops.api.vo.AppDependencyVO;
import com.ferry.devops.biz.service.IAppDependencyService;
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

import java.util.List;

/**
 * 应用依赖服务接口
 */
@RestController
@AllArgsConstructor
@RequestMapping("/app/dependency")
@Tag(name = "应用依赖服务", description = "应用依赖服务API接口控制器")
public class AppDependencyController {
    private final IAppDependencyService service;

    @GetMapping("/list")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "依赖服务列表")
    public R<List<AppDependencyVO>> dependencyList(@RequestParam("appId") Long appId) {
        return R.data(service.dependencyList(appId));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "依赖服务新增或编辑")
    public R<String> saveDependency(@RequestBody @Valid AppDependencyDTO body) {
        return service.saveDependency(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "依赖服务删除")
    public R<String> deletedDependency(@RequestBody List<Long> ids) {
        return service.deletedDependency(ids) ? R.success("删除成功") : R.fail("删除失败");
    }
}
