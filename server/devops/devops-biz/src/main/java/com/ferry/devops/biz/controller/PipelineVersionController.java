package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineVersionDTO;
import com.ferry.devops.api.vo.PipelineVersionVO;
import com.ferry.devops.biz.service.IPipelineVersionService;
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
@RequestMapping("/pipeline/version")
@Tag(name = "流水线版本", description = "流水线定义版本API接口控制器")
public class PipelineVersionController {
    private final IPipelineVersionService service;

    @GetMapping("/list")
    @PreAuthorize("devops:pipeline:version:list")
    @Operation(summary = "流水线版本-列表", description = "传入流水线id", parameters = {
            @Parameter(name = "pipelineId", description = "流水线id", required = true)
    })
    public R<List<PipelineVersionVO>> list(@RequestParam("pipelineId") Long pipelineId) {
        return R.data(service.list(pipelineId));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:pipeline:version:detail")
    @Operation(summary = "流水线版本-详情", description = "传入版本id", parameters = {
            @Parameter(name = "id", description = "版本id", required = true)
    })
    public R<PipelineVersionVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:pipeline:version:save")
    @Operation(summary = "流水线版本-保存定义", description = "保存定义并生成新版本")
    public R<String> save(@RequestBody @Valid PipelineVersionDTO body) {
        boolean isSuccess = service.save(body);
        return isSuccess ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/rollback")
    @PreAuthorize("devops:pipeline:rollback")
    @Operation(summary = "流水线版本-回退", description = "以目标版本内容生成新版本")
    public R<String> rollback(@RequestBody @Valid PipelineVersionDTO body) {
        boolean isSuccess = service.rollback(body);
        return isSuccess ? R.success("回退成功") : R.fail("回退失败");
    }
}
