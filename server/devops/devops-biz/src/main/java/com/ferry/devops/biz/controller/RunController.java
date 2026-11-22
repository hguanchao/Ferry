package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.cache.annotation.Idempotent;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.vo.PrecheckResultVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunStageTaskVO;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.TriggerResultVO;
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
@Tag(name = "运行管理", description = "运行详情、日志、干预与重跑API")
public class RunController {
    private final IRunService service;

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

    @GetMapping("/stage/list")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行-阶段列表")
    public R<List<RunStageVO>> stages(@RequestParam("id") Long runId) {
        return R.data(service.stages(runId));
    }

    @GetMapping("/task/list")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行-任务列表")
    public R<List<RunStageTaskVO>> tasks(@RequestParam("runStageId") Long runStageId) {
        return R.data(service.tasks(runStageId));
    }

    @GetMapping("/log")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行-日志分段")
    public R<?> log(@RequestParam("taskId") Long taskId,
                    @RequestParam(value = "fromLine", required = false) Integer fromLine,
                    @RequestParam(value = "limit", required = false) Integer limit) {
        return R.data(service.log(taskId, fromLine, limit));
    }

    @PostMapping("/log/search")
    @PreAuthorize("devops:run:detail")
    @Operation(summary = "运行-日志搜索")
    public R<?> logSearch(@RequestBody java.util.Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String keyword = (String) body.get("keyword");
        Integer fromLine = body.get("fromLine") == null ? null : Integer.valueOf(body.get("fromLine").toString());
        return R.data(service.logSearch(taskId, keyword, fromLine));
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
    @Idempotent(key = "'rerun:' + #body.id", ttl = 60)
    @Operation(summary = "运行-断点重跑（新建运行）")
    public R<TriggerResultVO> rerun(@RequestBody RunTriggerDTO body) {
        return R.data(service.rerun(body));
    }
}
