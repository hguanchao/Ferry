package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.RunOptionDTO;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.dto.query.RunTriggerRecordQuery;
import com.ferry.devops.api.vo.CommitBriefVO;
import com.ferry.devops.api.vo.PrecheckItemVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
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
@Tag(name = "流水线运行", description = "手动触发与运行台账API接口控制器")
public class RunController {
    private final IRunService service;

    @PostMapping("/branchOptions")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线运行-分支候选", description = "实时查询应用代码平台的分支列表")
    public R<List<String>> branchOptions(@RequestBody @Valid RunOptionDTO body) {
        return R.data(service.branchOptions(body));
    }

    @PostMapping("/commitOptions")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线运行-提交候选", description = "查询所选分支最近 10 条提交")
    public R<List<CommitBriefVO>> commitOptions(@RequestBody @Valid RunOptionDTO body) {
        return R.data(service.commitOptions(body));
    }

    @PostMapping("/precheck")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线运行-预校验", description = "五项预校验逐条结论，不落库；触发时服务端重跑并随运行落库")
    public R<List<PrecheckItemVO>> precheck(@RequestBody @Valid RunTriggerDTO body) {
        return R.data(service.precheck(body));
    }

    @PostMapping("/trigger")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线运行-手动触发", description = "以（应用、commit、流水线版本）为幂等键，命中进行中合并、命中成功待确认复用")
    public R<TriggerResultVO> trigger(@RequestBody @Valid RunTriggerDTO body) {
        return R.data(service.trigger(body));
    }

    @PostMapping("/trigger/pageList")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线运行-触发记录", description = "最近触发记录，含被拒绝的触发")
    public R<IPage<TriggerRecordVO>> triggerPageList(@RequestBody RunTriggerRecordQuery body) {
        return R.data(service.triggerPageList(body));
    }

    @PostMapping("/pageList")
    @PreAuthorize("devops:pipeline:runPageList")
    @Operation(summary = "流水线运行-分页列表", description = "按流水线或应用过滤")
    public R<IPage<RunVO>> pageList(@RequestBody RunQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:pipeline:runDetail")
    @Operation(summary = "流水线运行-详情", description = "传入运行id", parameters = {
            @Parameter(name = "id", description = "运行id", required = true)
    })
    public R<RunDetailVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/cancel")
    @PreAuthorize("devops:pipeline:runCancel")
    @Operation(summary = "流水线运行-取消", description = "仅排队中的运行可以取消", parameters = {
            @Parameter(name = "id", description = "运行id", required = true)
    })
    public R<String> cancel(@RequestParam("id") Long id) {
        boolean isSuccess = service.cancel(id);
        return isSuccess ? R.success("已取消") : R.fail("操作失败");
    }
}
