package com.ferry.system.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.RegionDTO;
import com.ferry.system.api.dto.query.RegionQuery;
import com.ferry.system.api.vo.tree.RegionTree;
import com.ferry.system.biz.service.IRegionService;
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
@RequestMapping("/region")
@Tag(name = "区域管理", description = "区域管理API接口控制器")
public class RegionController {
    private final IRegionService service;

    @PostMapping("/treeList")
    @PreAuthorize("system:region:treeList")
    @Operation(summary = "区域管理-树结构列表", description = "传入查询条件")
    public R<List<RegionTree>> treeList(@RequestBody RegionQuery body) {
        return R.data(service.treeList(body));
    }

    @PostMapping("/lazyList")
    @PreAuthorize("system:region:lazyList")
    @Operation(summary = "区域管理-懒加载列表", description = "传入查询条件")
    public R<List<RegionTree>> lazyList(@RequestBody RegionQuery body) {
        return R.data(service.lazyList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("system:region:detail")
    @Operation(summary = "区域管理-详情信息", description = "传入区域id", parameters = {
            @Parameter(name = "id", description = "区域id", required = true)
    })
    public R<RegionTree> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("system:region:save")
    @Operation(summary = "区域管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid RegionDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:region:deleted")
    @Operation(summary = "区域管理-删除信息", description = "传入区域id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }
}
