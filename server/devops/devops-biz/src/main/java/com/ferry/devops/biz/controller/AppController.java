package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AppCoreFlagApplyDTO;
import com.ferry.devops.api.dto.AppCoreFlagApproveDTO;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.AppDependencyDTO;
import com.ferry.devops.api.dto.AppRepoDTO;
import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.vo.AppDependencyVO;
import com.ferry.devops.api.vo.AppDetailVO;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.AppRepoVO;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.api.vo.WizardProgressVO;
import com.ferry.devops.biz.service.IAppService;
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
@RequestMapping("/app")
@Tag(name = "应用管理", description = "应用管理API接口控制器")
public class AppController {
    private final IAppService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:app:pageList")
    @Operation(summary = "应用管理-分页列表")
    public R<IPage<AppVO>> pageList(@RequestBody AppQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "应用管理-详情", parameters = @Parameter(name = "id", description = "应用id", required = true))
    public R<AppDetailVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "应用管理-新增或编辑")
    public R<String> save(@RequestBody @Valid AppDTO body) {
        boolean ok = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:app:deleted")
    @Operation(summary = "应用管理-删除")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @GetMapping("/options")
    @PreAuthorize("devops:app:option")
    @Operation(summary = "应用选项")
    public R<List<AppOptionVO>> options(@RequestParam(value = "keyword", required = false) String keyword,
                                        @RequestParam(value = "scope", required = false) Integer scope) {
        return R.data(service.options(keyword, scope));
    }

    @GetMapping("/repo/list")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "代码源列表")
    public R<List<AppRepoVO>> repoList(@RequestParam("appId") Long appId) {
        return R.data(service.repoList(appId));
    }

    @PostMapping("/repo/save")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "代码源新增或编辑")
    public R<String> saveRepo(@RequestBody @Valid AppRepoDTO body) {
        return service.saveRepo(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/repo/deleted")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "代码源删除")
    public R<String> deletedRepo(@RequestBody List<Long> ids) {
        return service.deletedRepo(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @GetMapping("/dependency/list")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "依赖服务列表")
    public R<List<AppDependencyVO>> dependencyList(@RequestParam("appId") Long appId) {
        return R.data(service.dependencyList(appId));
    }

    @PostMapping("/dependency/save")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "依赖服务新增或编辑")
    public R<String> saveDependency(@RequestBody @Valid AppDependencyDTO body) {
        return service.saveDependency(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/dependency/deleted")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "依赖服务删除")
    public R<String> deletedDependency(@RequestBody List<Long> ids) {
        return service.deletedDependency(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/coreFlag/apply")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "核心链路变更申请")
    public R<String> applyCoreFlag(@RequestBody @Valid AppCoreFlagApplyDTO body) {
        return service.applyCoreFlag(body) ? R.success("申请已提交") : R.fail("申请失败");
    }

    @PostMapping("/coreFlag/approve")
    @PreAuthorize("devops:app:coreFlagApprove")
    @Operation(summary = "核心链路变更审批")
    public R<String> approveCoreFlag(@RequestBody @Valid AppCoreFlagApproveDTO body) {
        return service.approveCoreFlag(body) ? R.success("审批完成") : R.fail("审批失败");
    }

    @PostMapping("/wizard/saveStep")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "接入向导保存步骤")
    public R<String> saveWizardStep(@RequestBody @Valid WizardStepDTO body) {
        return service.saveWizardStep(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @GetMapping("/wizard/progress")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "接入向导进度")
    public R<WizardProgressVO> wizardProgress(@RequestParam("id") Long id) {
        return R.data(service.wizardProgress(id));
    }

    @GetMapping("/aggregate/overview")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "应用概览")
    public R<AppOverviewVO> overview(@RequestParam("id") Long id) {
        return R.data(service.overview(id));
    }
}
