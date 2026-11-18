package com.ferry.devops.biz.controller;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.GateRuleDTO;
import com.ferry.devops.api.dto.query.GateRuleQuery;
import com.ferry.devops.api.vo.GateRuleVO;
import com.ferry.devops.biz.service.IGateRuleService;
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

/**
 * 门禁规则配置。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/gateRule")
@Tag(name = "门禁规则", description = "三作用域门禁规则配置与启停")
public class GateRuleController {

    private final IGateRuleService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:gateRule:pageList")
    @Operation(summary = "门禁规则-分页列表")
    public R<IPage<GateRuleVO>> pageList(@RequestBody GateRuleQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:gateRule:detail")
    @Operation(summary = "门禁规则-详情")
    public R<GateRuleVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:gateRule:save")
    @Operation(summary = "门禁规则-新增或编辑")
    public R<String> save(@RequestBody @Valid GateRuleDTO body) {
        return service.save(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:gateRule:deleted")
    @Operation(summary = "门禁规则-删除")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/enable")
    @PreAuthorize("devops:gateRule:enable")
    @Operation(summary = "门禁规则-启停")
    public R<String> enable(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        Object flag = body.get("isEnabled");
        Integer isEnabled = flag == null ? 1 : Integer.valueOf(String.valueOf(flag));
        return service.enable(id, isEnabled) ? R.success("操作成功") : R.fail("操作失败");
    }
}
