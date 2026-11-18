package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineTemplateDTO;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.dto.query.TemplateQuery;
import com.ferry.devops.api.vo.DerivationVO;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineTemplateVersionVO;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.api.vo.TemplateVariableVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.service.IPipelineTemplateService;
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
@RequestMapping("/pipelineTemplate")
@Tag(name = "流水线模板", description = "模板、版本、派生与合并API")
public class PipelineTemplateController {
    private final IPipelineTemplateService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:template:pageList")
    @Operation(summary = "模板-分页列表")
    public R<IPage<PipelineTemplateVO>> pageList(@RequestBody TemplateQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:template:detail")
    @Operation(summary = "模板-详情", parameters = @Parameter(name = "id", description = "模板id", required = true))
    public R<PipelineTemplateVersionVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:template:save")
    @Operation(summary = "模板-新增或编辑")
    public R<String> save(@RequestBody @Valid PipelineTemplateDTO body) {
        boolean ok = service.save(body);
        String msg = body.getId() == null ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:template:deleted")
    @Operation(summary = "模板-删除（有派生仅可停用）")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/enable")
    @PreAuthorize("devops:template:enable")
    @Operation(summary = "模板-启用")
    public R<String> enable(@RequestBody PipelineTemplateDTO body) {
        return service.enable(body.getId()) ? R.success("启用成功") : R.fail("启用失败");
    }

    @PostMapping("/disable")
    @PreAuthorize("devops:template:disable")
    @Operation(summary = "模板-停用")
    public R<String> disable(@RequestBody PipelineTemplateVersionDTO body) {
        return service.disable(body.getTemplateId(), body.getChangeNote()) ? R.success("停用成功") : R.fail("停用失败");
    }

    @GetMapping("/version/pageList")
    @PreAuthorize("devops:template:detail")
    @Operation(summary = "模板-版本列表")
    public R<IPage<VersionVO>> versionPage(@RequestParam("templateId") Long templateId,
                                           @RequestParam(value = "current", required = false) Long current,
                                           @RequestParam(value = "size", required = false) Long size) {
        return R.data(service.versionPage(templateId, current, size));
    }

    @GetMapping("/version/diff")
    @PreAuthorize("devops:template:detail")
    @Operation(summary = "模板-版本对比")
    public R<DiffVO> versionDiff(@RequestParam("leftId") Long leftId, @RequestParam("rightId") Long rightId) {
        return R.data(service.versionDiff(leftId, rightId));
    }

    @PostMapping("/version/rollback")
    @PreAuthorize("devops:template:save")
    @Operation(summary = "模板-版本回退")
    public R<VersionVO> rollback(@RequestBody @Valid PipelineTemplateVersionDTO body) {
        return R.data(service.rollback(body));
    }

    @GetMapping("/derivations")
    @PreAuthorize("devops:template:detail")
    @Operation(summary = "模板-派生关系")
    public R<List<DerivationVO>> derivations(@RequestParam("id") Long id) {
        return R.data(service.derivations(id));
    }

    @PostMapping("/merge")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "模板-逐条合并生成流水线新版本")
    public R<VersionVO> merge(@RequestBody @Valid PipelineTemplateVersionDTO body) {
        return R.data(service.merge(body));
    }

    @GetMapping("/variables")
    @PreAuthorize("devops:template:detail")
    @Operation(summary = "模板-占位变量")
    public R<List<TemplateVariableVO>> variables(@RequestParam("versionId") Long versionId) {
        return R.data(service.variables(versionId));
    }
}
