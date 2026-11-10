package com.ferry.system.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.RoleDTO;
import com.ferry.system.api.dto.query.RoleQuery;
import com.ferry.system.api.vo.RoleVO;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/role")
@Tag(name = "角色管理", description = "角色管理API接口控制器")
public class RoleController {
    private final IRoleService service;

    @PostMapping("/pageList")
    @PreAuthorize("system:role:pageList")
    @Operation(summary = "角色管理-分页列表", description = "传入查询条件")
    public R<IPage<RoleVO>> pageList(@RequestBody RoleQuery body) {
        return R.data(service.pageList(body));
    }

    @PostMapping("/save")
    @PreAuthorize("system:role:save")
    @Operation(summary = "角色管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid RoleDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:role:deleted")
    @Operation(summary = "角色管理-删除信息", description = "传入角色id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }

    @GetMapping("/menuList")
    @PreAuthorize("system:role:menuList")
    @Operation(summary = "角色管理-角色菜单列表", description = "传入角色id", parameters = {
            @Parameter(name = "id", description = "角色id", required = true)
    })
    public R<List<MenuTree>> menuList(@RequestParam("id") Long id) {
        return R.data(service.menuList(id));
    }

    @PostMapping("/authorizeMenu")
    @PreAuthorize("system:role:authorizeMenu")
    @Operation(summary = "角色管理-角色授权菜单", description = "传入角色id和菜单id")
    public R<String> authorizeMenu(@RequestBody RoleAuthorize<Long, Long> body) {
        boolean isSuccess = service.authorizeMenu(body);
        return isSuccess ? R.success("授权成功") : R.fail("授权失败");
    }
}
