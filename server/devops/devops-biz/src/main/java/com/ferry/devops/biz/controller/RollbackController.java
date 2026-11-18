package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.RollbackDTO;
import com.ferry.devops.api.dto.query.RollbackQuery;
import com.ferry.devops.api.vo.RollbackCandidateVO;
import com.ferry.devops.api.vo.RollbackVO;
import com.ferry.devops.biz.service.IRollbackService;
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
import java.util.Map;

/**
 * 回滚控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/rollback")
@Tag(name = "回滚管理", description = "健康版本回滚与候选版本")
public class RollbackController {

    private final IRollbackService service;

    @PostMapping("/start")
    @PreAuthorize("devops:rollback:start")
    @Operation(summary = "发起回滚")
    public R<Map<String, Object>> start(@RequestBody @Valid RollbackDTO body) {
        Long id = service.start(body);
        return R.data(Map.of("id", id == null ? "" : id));
    }

    @PostMapping("/pageList")
    @PreAuthorize("devops:rollback:pageList")
    @Operation(summary = "回滚记录分页列表")
    public R<IPage<RollbackVO>> pageList(@RequestBody RollbackQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/candidates")
    @PreAuthorize("devops:rollback:candidates")
    @Operation(summary = "健康版本候选", parameters = @Parameter(name = "envId", description = "环境id", required = true))
    public R<List<RollbackCandidateVO>> candidates(@RequestParam("envId") Long envId) {
        return R.data(service.candidates(envId));
    }
}
