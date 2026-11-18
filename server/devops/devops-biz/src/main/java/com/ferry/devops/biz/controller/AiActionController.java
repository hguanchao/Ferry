package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.ActionExecuteDTO;
import com.ferry.devops.api.dto.ActionProposeDTO;
import com.ferry.devops.api.dto.query.ActionRecordQuery;
import com.ferry.devops.api.vo.ActionCatalogVO;
import com.ferry.devops.api.vo.ActionRecordVO;
import com.ferry.devops.biz.service.IActionService;
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

import java.util.List;

/**
 * AI 动作引擎控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ai/action")
@Tag(name = "AI 动作引擎", description = "动作目录、提议、执行、确认与撤销")
public class AiActionController {
    private final IActionService service;

    @GetMapping("/catalog")
    @PreAuthorize("devops:action:catalog")
    @Operation(summary = "动作目录(仅启用项)", parameters = @Parameter(name = "level", description = "动作级别", required = false))
    public R<List<ActionCatalogVO>> catalog(@RequestParam(value = "level", required = false) Integer level) {
        return R.data(service.catalog(level));
    }

    @PostMapping("/propose")
    @PreAuthorize("devops:action:propose")
    @Operation(summary = "提议动作")
    public R<ActionRecordVO> propose(@RequestBody @Valid ActionProposeDTO body) {
        return R.data(service.propose(body));
    }

    @PostMapping("/execute")
    @PreAuthorize("devops:action:execute")
    @Operation(summary = "执行动作(L1 直接执行，L2 转待确认)")
    public R<ActionRecordVO> execute(@RequestBody @Valid ActionExecuteDTO body) {
        return R.data(service.execute(body));
    }

    @PostMapping("/confirm/{id}")
    @PreAuthorize("devops:action:execute")
    @Operation(summary = "确认执行 L2 动作")
    public R<String> confirm(@PathVariable("id") Long id) {
        ActionExecuteDTO body = new ActionExecuteDTO();
        body.setRecordId(id);
        body.setImpactConfirmed(true);
        service.confirm(body);
        return R.success("已确认执行");
    }

    @PostMapping("/undo/{id}")
    @PreAuthorize("devops:action:undo")
    @Operation(summary = "撤销动作(10 分钟内)")
    public R<String> undo(@PathVariable("id") Long id) {
        return service.undo(id) ? R.success("撤销成功") : R.fail("撤销失败");
    }

    @PostMapping("/records")
    @PreAuthorize("devops:action:record")
    @Operation(summary = "动作记录分页")
    public R<IPage<ActionRecordVO>> records(@RequestBody ActionRecordQuery body) {
        return R.data(service.records(body));
    }
}
