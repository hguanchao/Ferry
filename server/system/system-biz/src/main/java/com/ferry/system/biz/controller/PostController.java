package com.ferry.system.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.PostDTO;
import com.ferry.system.api.dto.query.PostQuery;
import com.ferry.system.api.vo.PostVO;
import com.ferry.system.biz.service.IPostService;
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
@RequestMapping("/post")
@Tag(name = "岗位管理", description = "岗位管理API接口控制器")
public class PostController {
    private final IPostService service;

    @PostMapping("/pageList")
    @PreAuthorize("system:post:pageList")
    @Operation(summary = "岗位管理-分页列表", description = "传入查询条件")
    public R<IPage<PostVO>> pageList(@RequestBody PostQuery body) {
        return R.data(service.pageList(body));
    }

    @PostMapping("/save")
    @PreAuthorize("system:post:save")
    @Operation(summary = "岗位管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid PostDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:post:deleted")
    @Operation(summary = "岗位管理-删除信息", description = "传入岗位id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }
}
