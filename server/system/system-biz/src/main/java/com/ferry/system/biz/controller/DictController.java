package com.ferry.system.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.DictDTO;
import com.ferry.system.api.dto.query.DictQuery;
import com.ferry.system.api.vo.DictVO;
import com.ferry.system.api.vo.tree.DictTree;
import com.ferry.system.biz.service.IDictService;
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

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/dict")
@Tag(name = "字典管理", description = "字典管理API接口控制器")
public class DictController {
    private final IDictService service;

    @PostMapping("/treeList")
    @PreAuthorize("system:dict:treeList")
    @Operation(summary = "字典管理-树形列表", description = "传入查询条件")
    public R<List<DictTree>> treeList(@RequestBody DictQuery body) {
        return R.data(service.treeList(body));
    }

    @PostMapping("/pageList")
    @PreAuthorize("system:dict:pageList")
    @Operation(summary = "字典管理-分页列表", description = "传入查询条件")
    public R<IPage<DictVO>> pageList(@RequestBody DictQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("system:dict:detail")
    @Operation(summary = "字典管理-详情信息", description = "传入字典id", parameters = {
            @Parameter(name = "id", description = "字典id", required = true)
    })
    public R<DictTree> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("system:dict:save")
    @Operation(summary = "字典管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody DictDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:dict:deleted")
    @Operation(summary = "字典管理-删除信息", description = "传入字典id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }
}
