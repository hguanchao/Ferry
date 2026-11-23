package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelinePreviewDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelinePreviewVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.ValidateResultVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.service.IPipelineService;
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
@RequestMapping("/pipeline")
@Tag(name = "流水线管理", description = "流水线定义与版本API")
public class PipelineController {
    private final IPipelineService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:pipeline:pageList")
    @Operation(summary = "流水线-分页列表")
    public R<IPage<PipelineVO>> pageList(@RequestBody PipelineQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:pipeline:detail")
    @Operation(summary = "流水线-详情", parameters = @Parameter(name = "id", description = "流水线id", required = true))
    public R<PipelineVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-新增或编辑（保存后同步 Jenkins Job）")
    public R<String> save(@RequestBody @Valid PipelineDTO body) {
        boolean isSuccess = service.save(body);
        String title = ObjUtil.isEmpty(body.getId()) ? "新增" : "编辑";
        return isSuccess ? R.success(title + "成功") : R.fail(title + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:pipeline:deleted")
    @Operation(summary = "流水线-删除")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/changeStatus")
    @PreAuthorize("devops:pipeline:changeStatus")
    @Operation(summary = "流水线-变更状态")
    public R<String> changeStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        boolean isSuccess = service.changeStatus(id, status);
        String title = PipelineStatusEnum.ENABLED.getCode().equals(status) ? "启用" : "停用";
        return isSuccess ? R.success(title + "成功") : R.fail(title + "失败");
    }

    @PostMapping("/sync")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-Jenkins Job 同步（保存/启停同步失败后的重试入口）")
    public R<String> sync(@RequestBody PipelineDTO body) {
        return R.data(service.sync(body.getId()));
    }

    @PostMapping("/definition/validate")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-定义校验")
    public R<ValidateResultVO> validateDefinition(@RequestBody java.util.Map<String, Object> body) {
        String definition = (String) body.get("definition");
        Integer mode = body.get("mode") == null ? null : Integer.valueOf(body.get("mode").toString());
        return R.data(service.validateDefinition(definition, mode));
    }

    @PostMapping("/definition/preview")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-脚本预览")
    public R<PipelinePreviewVO> previewDefinition(@RequestBody @Valid PipelinePreviewDTO body) {
        return R.data(service.previewDefinition(body.getDefinition(), body.getMode()));
    }

    @PostMapping("/definition/save")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-定义保存生成新版本")
    public R<VersionVO> saveDefinition(@RequestBody java.util.Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String definition = (String) body.get("definition");
        Integer mode = body.get("mode") == null ? 1 : Integer.valueOf(body.get("mode").toString());
        String changeNote = (String) body.get("changeNote");
        return R.data(service.saveDefinition(id, definition, mode, changeNote));
    }

    @GetMapping("/version/pageList")
    @PreAuthorize("devops:pipeline:detail")
    @Operation(summary = "流水线-版本列表")
    public R<IPage<VersionVO>> versionPage(@RequestParam("pipelineId") Long pipelineId,
                                           @RequestParam(value = "current", required = false) Long current,
                                           @RequestParam(value = "size", required = false) Long size) {
        return R.data(service.versionPage(pipelineId, current, size));
    }

    @GetMapping("/version/diff")
    @PreAuthorize("devops:pipeline:detail")
    @Operation(summary = "流水线-版本对比")
    public R<DiffVO> versionDiff(@RequestParam("leftId") Long leftId, @RequestParam("rightId") Long rightId) {
        return R.data(service.versionDiff(leftId, rightId));
    }

    @PostMapping("/version/rollback")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-版本回退（复制成新版本）")
    public R<VersionVO> rollback(@RequestBody java.util.Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        Long versionId = Long.valueOf(body.get("versionId").toString());
        String changeNote = (String) body.get("changeNote");
        return R.data(service.rollback(id, versionId, changeNote));
    }

}
