package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.cache.annotation.Idempotent;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.RepoCommitVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.biz.service.IRepoQueryService;
import com.ferry.devops.biz.service.IRunService;
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

@RestController
@AllArgsConstructor
@RequestMapping("/run")
@Tag(name = "运行管理", description = "运行触发、详情、日志、干预与重跑API")
public class RunController {
    private final IRunService service;
    private final IRepoQueryService repoQueryService;

    @PostMapping("/pageList")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行-分页列表")
    public R<IPage<RunVO>> pageList(@RequestBody RunQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行-详情", parameters = @Parameter(name = "id", description = "运行id", required = true))
    public R<RunDetailVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/pause")
    @PreAuthorize("devops:run:pause")
    @Operation(summary = "运行-暂停")
    public R<String> pause(@RequestBody RunTriggerDTO body) {
        return service.pause(body.getId(), body.getReason()) ? R.success("已暂停") : R.fail("暂停失败");
    }

    @PostMapping("/resume")
    @PreAuthorize("devops:run:resume")
    @Operation(summary = "运行-继续")
    public R<String> resume(@RequestBody RunTriggerDTO body) {
        return service.resume(body.getId()) ? R.success("已继续") : R.fail("继续失败");
    }

    @PostMapping("/abort")
    @PreAuthorize("devops:run:abort")
    @Operation(summary = "运行-中止")
    public R<String> abort(@RequestBody RunTriggerDTO body) {
        return service.abort(body.getId(), body.getReason()) ? R.success("已中止") : R.fail("中止失败");
    }

    @GetMapping("/rerunPlan")
    @PreAuthorize("devops:run:rerun")
    @Operation(summary = "运行-断点重跑计划")
    public R<List<RunStageVO>> rerunPlan(@RequestParam("id") Long id) {
        return R.data(service.rerunPlan(id));
    }

    @PostMapping("/rerun")
    @PreAuthorize("devops:run:rerun")
    @Idempotent(key = "'rerun:' + #body.id")
    @Operation(summary = "运行-断点重跑（新建运行）")
    public R<TriggerResultVO> rerun(@RequestBody RunTriggerDTO body) {
        return R.data(service.rerun(body));
    }

    @GetMapping("/trigger/branches")
    @PreAuthorize("devops:run:trigger")
    @Operation(summary = "运行-触发弹窗分支候选")
    public R<List<String>> triggerBranches(@RequestParam("appId") Long appId) {
        return R.data(repoQueryService.branches(appId));
    }

    @GetMapping("/trigger/commits")
    @PreAuthorize("devops:run:trigger")
    @Operation(summary = "运行-触发弹窗提交候选")
    public R<List<RepoCommitVO>> triggerCommits(@RequestParam("appId") Long appId,
                                                @RequestParam("branch") String branch,
                                                @RequestParam(value = "limit", required = false) Integer limit) {
        return R.data(repoQueryService.commits(appId, branch, limit));
    }

    @PostMapping("/trigger")
    @PreAuthorize("devops:run:trigger")
    @Idempotent(key = "#body.appId + ':' + #body.pipelineId + ':' + (#body.commitId ?: '')")
    @Operation(summary = "运行-手动触发（幂等）")
    public R<TriggerResultVO> trigger(@RequestBody @Valid RunTriggerDTO body) {
        return R.data(service.trigger(body));
    }
}
