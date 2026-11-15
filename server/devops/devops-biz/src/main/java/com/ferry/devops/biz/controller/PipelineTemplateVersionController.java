package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.vo.PipelineTemplateVersionVO;
import com.ferry.devops.biz.service.IPipelineTemplateVersionService;
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
@RequestMapping("/pipeline/template/version")
@Tag(name = "流水线模板版本", description = "流水线模板定义版本API接口控制器")
public class PipelineTemplateVersionController {
    private final IPipelineTemplateVersionService service;

    @GetMapping("/list")
    @PreAuthorize("devops:template:version:list")
    @Operation(summary = "流水线模板版本-列表", description = "传入模板id", parameters = {
            @Parameter(name = "templateId", description = "模板id", required = true)
    })
    public R<List<PipelineTemplateVersionVO>> list(@RequestParam("templateId") Long templateId) {
        return R.data(service.list(templateId));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:template:version:detail")
    @Operation(summary = "流水线模板版本-详情", description = "传入版本id", parameters = {
            @Parameter(name = "id", description = "版本id", required = true)
    })
    public R<PipelineTemplateVersionVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:template:version:save")
    @Operation(summary = "流水线模板版本-保存定义", description = "保存定义并生成新版本")
    public R<String> save(@RequestBody @Valid PipelineTemplateVersionDTO body) {
        boolean isSuccess = service.save(body);
        return isSuccess ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/rollback")
    @PreAuthorize("devops:template:rollback")
    @Operation(summary = "流水线模板版本-回退", description = "以目标版本内容生成新版本")
    public R<String> rollback(@RequestBody @Valid PipelineTemplateVersionDTO body) {
        boolean isSuccess = service.rollback(body);
        return isSuccess ? R.success("回退成功") : R.fail("回退失败");
    }
}
