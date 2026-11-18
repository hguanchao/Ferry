package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.ArtifactCleanPolicyDTO;
import com.ferry.devops.api.vo.CleanPolicyVO;
import com.ferry.devops.api.vo.CleanTaskVO;
import com.ferry.devops.biz.service.IArtifactCleanService;
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
 * 制品清理策略与任务。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/artifactClean")
@Tag(name = "制品清理", description = "清理策略配置、任务清单与执行")
public class ArtifactCleanController {

    private final IArtifactCleanService service;

    @GetMapping("/policy")
    @PreAuthorize("devops:artifactClean:detail")
    @Operation(summary = "清理策略-详情")
    public R<CleanPolicyVO> policy(@RequestParam("appId") Long appId) {
        return R.data(service.policy(appId));
    }

    @PostMapping("/policy/save")
    @PreAuthorize("devops:artifactClean:save")
    @Operation(summary = "清理策略-保存")
    public R<String> savePolicy(@RequestBody @Valid ArtifactCleanPolicyDTO body) {
        return service.savePolicy(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/run")
    @PreAuthorize("devops:artifactClean:run")
    @Operation(summary = "清理-生成清单或执行")
    public R<String> run(@RequestParam("appId") Long appId,
                         @RequestParam(value = "confirm", required = false) Boolean confirm) {
        return service.run(appId, confirm) ? R.success("操作成功") : R.fail("操作失败");
    }

    /** 前端 artifactApi.cleanRuns 使用 GET /runs，返回任务列表。 */
    @GetMapping("/runs")
    @PreAuthorize("devops:artifactClean:pageList")
    @Operation(summary = "清理任务-列表")
    public R<List<CleanTaskVO>> runs(@RequestParam("appId") Long appId) {
        IPage<CleanTaskVO> page = service.taskPage(appId, 1, 30);
        return R.data(page.getRecords());
    }

    @GetMapping("/task/pageList")
    @PreAuthorize("devops:artifactClean:pageList")
    @Operation(summary = "清理任务-分页列表")
    public R<IPage<CleanTaskVO>> taskPage(@RequestParam("appId") Long appId,
                                          @RequestParam(value = "current", required = false) Integer current,
                                          @RequestParam(value = "size", required = false) Integer size) {
        return R.data(service.taskPage(appId, current, size));
    }

    @GetMapping("/task/manifest")
    @PreAuthorize("devops:artifactClean:detail")
    @Operation(summary = "清理任务-清单下载")
    public R<String> manifest(@RequestParam("taskId") Long taskId) {
        return R.data(service.manifest(taskId));
    }
}
