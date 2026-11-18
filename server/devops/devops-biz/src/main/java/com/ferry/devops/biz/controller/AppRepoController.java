package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AppRepoDTO;
import com.ferry.devops.api.vo.AppRepoVO;
import com.ferry.devops.biz.service.IAppRepoService;
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
 * 应用代码源接口
 */
@RestController
@AllArgsConstructor
@RequestMapping("/app/repo")
@Tag(name = "应用代码源", description = "应用代码源API接口控制器")
public class AppRepoController {
    private final IAppRepoService service;

    @GetMapping("/list")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "代码源列表")
    public R<List<AppRepoVO>> repoList(@RequestParam("appId") Long appId) {
        return R.data(service.repoList(appId));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "代码源新增或编辑")
    public R<String> saveRepo(@RequestBody @Valid AppRepoDTO body) {
        return service.saveRepo(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "代码源删除")
    public R<String> deletedRepo(@RequestBody List<Long> ids) {
        return service.deletedRepo(ids) ? R.success("删除成功") : R.fail("删除失败");
    }
}
