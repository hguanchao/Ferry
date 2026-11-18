package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.ReleaseApproveDTO;
import com.ferry.devops.api.dto.ReleaseArtifactDTO;
import com.ferry.devops.api.dto.ReleaseCandidateDTO;
import com.ferry.devops.api.dto.ReleaseCheckDTO;
import com.ferry.devops.api.dto.ReleaseTerminateDTO;
import com.ferry.devops.api.dto.query.ReleaseQuery;
import com.ferry.devops.api.vo.ArtifactCandidateVO;
import com.ferry.devops.api.vo.ImpactVO;
import com.ferry.devops.api.vo.ReleaseCheckVO;
import com.ferry.devops.api.vo.ReleaseDetailVO;
import com.ferry.devops.api.vo.ReleaseRecordVO;
import com.ferry.devops.api.vo.ReleaseTimelineVO;
import com.ferry.devops.api.vo.ReleaseVO;
import com.ferry.devops.api.vo.RiskVO;
import com.ferry.devops.biz.service.IReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 发布单与生产发布控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/release")
@Tag(name = "发布准入与生产发布", description = "发布单、影响面、风险、准入审批与生产发布编排")
public class ReleaseController {

    private final IReleaseService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:release:pageList")
    @Operation(summary = "发布单分页列表")
    public R<IPage<ReleaseVO>> pageList(@RequestBody ReleaseQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:release:detail")
    @Operation(summary = "发布单详情", parameters = @Parameter(name = "id", description = "发布单id", required = true))
    public R<ReleaseDetailVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:release:save")
    @Operation(summary = "新增或编辑发布单")
    public R<String> save(@RequestBody @Valid ReleaseCandidateDTO body) {
        boolean ok = service.save(body);
        String msg = body.getId() == null ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:release:save")
    @Operation(summary = "删除发布单（仅草稿）")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @GetMapping("/artifacts/candidates")
    @PreAuthorize("devops:release:save")
    @Operation(summary = "发布单可选制品")
    public R<List<ArtifactCandidateVO>> candidates(@RequestParam("appId") Long appId) {
        return R.data(service.candidates(appId));
    }

    @PostMapping("/artifacts/save")
    @PreAuthorize("devops:release:save")
    @Operation(summary = "保存发布单制品")
    public R<String> saveArtifacts(@RequestBody @Valid ReleaseArtifactDTO body) {
        return service.saveArtifacts(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/submit/{id}")
    @PreAuthorize("devops:release:submit")
    @Operation(summary = "提交准入（锁定制品）")
    public R<String> submit(@PathVariable("id") Long id) {
        return service.submit(id) ? R.success("提交成功") : R.fail("提交失败");
    }

    @PostMapping("/withdraw/{id}")
    @PreAuthorize("devops:release:submit")
    @Operation(summary = "撤回发布单")
    public R<String> withdraw(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.withdraw(id, String.valueOf(body.getOrDefault("reason", ""))) ? R.success("撤回成功") : R.fail("撤回失败");
    }

    @GetMapping("/impact")
    @PreAuthorize("devops:release:detail")
    @Operation(summary = "三层影响面")
    public R<ImpactVO> impact(@RequestParam("id") Long id) {
        return R.data(service.impact(id));
    }

    @GetMapping("/risk")
    @PreAuthorize("devops:release:detail")
    @Operation(summary = "五因子风险评级")
    public R<RiskVO> risk(@RequestParam("id") Long id) {
        return R.data(service.risk(id));
    }

    @GetMapping("/check/list")
    @PreAuthorize("devops:release:detail")
    @Operation(summary = "六项准入检查")
    public R<List<ReleaseCheckVO>> checks(@RequestParam("id") Long id) {
        return R.data(service.checks(id));
    }

    @PostMapping("/check/save")
    @PreAuthorize("devops:release:approve")
    @Operation(summary = "保存人工检查结论")
    public R<String> saveChecks(@RequestBody @Valid ReleaseCheckDTO body) {
        return service.saveCheck(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/approve")
    @PreAuthorize("devops:release:approve")
    @Operation(summary = "审批通过")
    public R<String> approve(@RequestBody @Valid ReleaseApproveDTO body) {
        return service.approve(body) ? R.success("审批完成") : R.fail("审批失败");
    }

    @PostMapping("/reject")
    @PreAuthorize("devops:release:approve")
    @Operation(summary = "驳回")
    public R<String> reject(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        return service.reject(id, String.valueOf(body.getOrDefault("opinion", "")),
                String.valueOf(body.getOrDefault("returnStage", ""))) ? R.success("已驳回") : R.fail("操作失败");
    }

    @GetMapping("/timeline")
    @PreAuthorize("devops:release:detail")
    @Operation(summary = "发布单时间线")
    public R<List<ReleaseTimelineVO>> timeline(@RequestParam("id") Long id) {
        return R.data(service.timeline(id));
    }

    @PostMapping("/start/{id}")
    @PreAuthorize("devops:release:start")
    @Operation(summary = "启动生产发布（需 devops:release:prod 权限校验）")
    public R<Map<String, Object>> start(@PathVariable("id") Long id,
                                        @RequestBody(required = false) Map<String, Object> body) {
        Long confirmer = body == null || body.get("confirmerId") == null
                ? null : Long.valueOf(String.valueOf(body.get("confirmerId")));
        Long recordId = service.start(id, confirmer);
        return R.data(Map.of("id", recordId == null ? "" : recordId));
    }

    @PostMapping("/terminate")
    @PreAuthorize("devops:release:start")
    @Operation(summary = "终止发布（不回滚）")
    public R<String> terminate(@RequestBody @Valid ReleaseTerminateDTO body) {
        return service.terminate(body) ? R.success("已终止") : R.fail("操作失败");
    }

    @PostMapping("/record/pageList")
    @PreAuthorize("devops:release:record")
    @Operation(summary = "发布记录分页列表")
    public R<IPage<ReleaseRecordVO>> recordPage(@RequestBody ReleaseQuery body) {
        return R.data(service.recordPage(body));
    }
}
