package com.ferry.system.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.MenuDTO;
import com.ferry.system.api.dto.query.MenuQuery;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.service.IMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/menu")
@Tag(name = "菜单管理", description = "菜单管理API接口控制器")
public class MenuController {
    private final IMenuService service;

    @PostMapping("/treeList")
    @PreAuthorize("system:menu:treeList")
    @Operation(summary = "菜单管理-树形列表", description = "传入查询条件")
    public R<List<MenuTree>> treeList(@RequestBody MenuQuery body) {
        return R.data(service.treeList(body));
    }

    @PostMapping("/save")
    @PreAuthorize("system:menu:save")
    @Operation(summary = "菜单管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid MenuDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:menu:deleted")
    @Operation(summary = "菜单管理-删除信息", description = "传入菜单id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }
}
