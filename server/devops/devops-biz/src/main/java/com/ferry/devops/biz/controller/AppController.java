package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.biz.service.IAppService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * 应用档案接口（应用本体、选项、详情）
 */
@RestController
@AllArgsConstructor
@RequestMapping("/app")
@Tag(name = "应用管理", description = "应用档案API接口控制器")
public class AppController {
    private final IAppService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:app:pageList")
    @Operation(summary = "应用管理-分页列表")
    public R<IPage<AppVO>> pageList(@RequestBody AppQuery body) {
        return R.data(service.pageList(body));
    }

    // TODO(dev_app-schema): /app/detail 已移除——详情与分页返回同一 AppVO，前端复用列表数据，无独立调用方。

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
}
