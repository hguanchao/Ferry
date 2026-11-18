package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.ChangeTicketDTO;
import com.ferry.devops.api.dto.query.ChangeTicketQuery;
import com.ferry.devops.api.vo.ChangeTicketVO;
import com.ferry.devops.biz.service.IChangeTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 变更单控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/changeTicket")
@Tag(name = "变更单管理", description = "变更单维护、审批与工单协同")
public class ChangeTicketController {

    private final IChangeTicketService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:changeTicket:pageList")
    @Operation(summary = "变更单分页列表")
    public R<IPage<ChangeTicketVO>> pageList(@RequestBody ChangeTicketQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:changeTicket:detail")
    @Operation(summary = "变更单详情", parameters = @Parameter(name = "id", description = "变更单id", required = true))
    public R<ChangeTicketVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:changeTicket:save")
    @Operation(summary = "新增或编辑变更单")
    public R<String> save(@RequestBody @Valid ChangeTicketDTO body) {
        boolean ok = service.save(body);
        String msg = body.getId() == null ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/submit/{id}")
    @PreAuthorize("devops:changeTicket:submit")
    @Operation(summary = "提交审批")
    public R<String> submit(@PathVariable("id") Long id) {
        return service.submit(id) ? R.success("提交成功") : R.fail("提交失败");
    }

    @PostMapping("/approve")
    @PreAuthorize("devops:changeTicket:approve")
    @Operation(summary = "审批")
    public R<String> approve(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        Boolean pass = Boolean.valueOf(String.valueOf(body.get("pass")));
        String opinion = String.valueOf(body.getOrDefault("opinion", ""));
        return service.approve(id, pass, opinion) ? R.success("审批完成") : R.fail("审批失败");
    }

    @PostMapping("/close/{id}")
    @PreAuthorize("devops:changeTicket:close")
    @Operation(summary = "关闭变更单")
    public R<String> close(@PathVariable("id") Long id) {
        return service.close(id) ? R.success("关闭成功") : R.fail("关闭失败");
    }

    @PostMapping("/cancel/{id}")
    @PreAuthorize("devops:changeTicket:cancel")
    @Operation(summary = "取消变更单")
    public R<String> cancel(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> body) {
        return service.cancel(id, body == null ? "" : String.valueOf(body.getOrDefault("reason", ""))) ? R.success("取消成功") : R.fail("取消失败");
    }
}
