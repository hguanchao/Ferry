package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.EnvironmentDTO;
import com.ferry.devops.api.dto.EnvTypeChangeApplyDTO;
import com.ferry.devops.api.dto.EnvTypeChangeApproveDTO;
import com.ferry.devops.api.dto.query.EnvironmentQuery;
import com.ferry.devops.api.vo.EnvironmentVO;
import com.ferry.devops.api.vo.EnvOptionVO;
import com.ferry.devops.biz.service.IEnvironmentService;
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
@RequestMapping("/environment")
@Tag(name = "环境管理", description = "环境管理API接口控制器")
public class EnvironmentController {
    private final IEnvironmentService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:environment:pageList")
    @Operation(summary = "环境管理-分页列表")
    public R<IPage<EnvironmentVO>> pageList(@RequestBody EnvironmentQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:environment:detail")
    @Operation(summary = "环境管理-详情", parameters = @Parameter(name = "id", description = "环境id", required = true))
    public R<EnvironmentVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:environment:save")
    @Operation(summary = "环境管理-新增或编辑")
    public R<String> save(@RequestBody @Valid EnvironmentDTO body) {
        boolean ok = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:environment:deleted")
    @Operation(summary = "环境管理-删除")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @GetMapping("/options")
    @PreAuthorize("devops:environment:option")
    @Operation(summary = "环境选项")
    public R<List<EnvOptionVO>> options(@RequestParam(value = "appId", required = false) Long appId) {
        return R.data(service.options(appId));
    }

    @PostMapping("/changeType/apply")
    @PreAuthorize("devops:environment:changeType")
    @Operation(summary = "部署类型变更申请")
    public R<String> applyTypeChange(@RequestBody @Valid EnvTypeChangeApplyDTO body) {
        return service.applyTypeChange(body) ? R.success("申请已提交") : R.fail("申请失败");
    }

    @PostMapping("/changeType/approve")
    @PreAuthorize("devops:environment:changeTypeApprove")
    @Operation(summary = "部署类型变更审批")
    public R<String> approveTypeChange(@RequestBody @Valid EnvTypeChangeApproveDTO body) {
        return service.approveTypeChange(body) ? R.success("审批完成") : R.fail("审批失败");
    }
}
