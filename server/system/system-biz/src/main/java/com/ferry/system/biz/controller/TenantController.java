package com.ferry.system.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.TenantDTO;
import com.ferry.system.api.dto.query.TenantQuery;
import com.ferry.system.api.vo.TenantVO;
import com.ferry.system.biz.service.ITenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/tenant")
@Tag(name = "租户管理", description = "租户管理API接口控制器")
public class TenantController {
    private final ITenantService service;

    /**
     * 租户列表不标注权限注解：该路径在免登录名单中，供登录页选择租户使用，
     * 标注后取不到会话会直接返回 401，登录页将无法加载。
     */
    @PostMapping("/list")
    @Operation(summary = "租户管理-租户列表", description = "传入查询条件")
    public R<List<TenantVO>> list(@RequestBody TenantQuery body) {
        return R.data(service.list(body));
    }

    @PostMapping("/pageList")
    @PreAuthorize("system:tenant:pageList")
    @Operation(summary = "租户管理-分页列表", description = "传入查询条件")
    public R<IPage<TenantVO>> pageList(@RequestBody TenantQuery body) {
        return R.data(service.pageList(body));
    }

    @PostMapping("/save")
    @PreAuthorize("system:tenant:save")
    @Operation(summary = "租户管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid TenantDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:tenant:deleted")
    @Operation(summary = "租户管理-删除信息", description = "传入租户id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");

    }
}
