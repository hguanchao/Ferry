package com.ferry.system.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.ConfigDTO;
import com.ferry.system.api.dto.query.ConfigQuery;
import com.ferry.system.api.vo.ConfigVO;
import com.ferry.system.biz.service.IConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/config")
@Tag(name = "配置管理", description = "配置管理API接口控制器")
public class ConfigController {
    private final IConfigService service;

    @PostMapping("/pageList")
    @PreAuthorize("system:config:pageList")
    @Operation(summary = "配置管理-分页列表", description = "传入查询条件")
    public R<IPage<ConfigVO>> pageList(@RequestBody ConfigQuery body) {
        return R.data(service.pageList(body));
    }

    @PostMapping("/save")
    @PreAuthorize("system:config:save")
    @Operation(summary = "配置管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody ConfigDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:config:deleted")
    @Operation(summary = "配置管理-删除信息", description = "传入配置id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");

    }
}
