package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.vo.AppVO;
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
    @Operation(summary = "应用管理-分页列表", description = "传入查询条件")
    public R<IPage<AppVO>> pageList(@RequestBody AppQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:app:detail")
    @Operation(summary = "应用管理-详情", description = "传入应用id", parameters = {
            @Parameter(name = "id", description = "应用id", required = true)
    })
    public R<AppVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:app:save")
    @Operation(summary = "应用管理-新增或编辑", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid AppDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:app:deleted")
    @Operation(summary = "应用管理-逻辑删除", description = "传入应用id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }
}
