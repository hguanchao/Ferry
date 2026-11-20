package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.PrecheckResultVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.api.vo.ValidateResultVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IRunService;
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
@Tag(name = "流水线管理", description = "流水线定义、版本与触发API")
public class PipelineController {
    private final IPipelineService service;
    private final IRunService runService;

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
    @Operation(summary = "流水线-新增或编辑")
    public R<String> save(@RequestBody @Valid PipelineDTO body) {
        boolean ok = service.save(body);
        String msg = body.getId() == null ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:pipeline:deleted")
    @Operation(summary = "流水线-删除")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/enable")
    @PreAuthorize("devops:pipeline:enable")
    @Operation(summary = "流水线-启用")
    public R<String> enable(@RequestBody PipelineDTO body) {
        return service.enable(body.getId()) ? R.success("启用成功") : R.fail("启用失败");
    }

    @PostMapping("/disable")
    @PreAuthorize("devops:pipeline:disable")
    @Operation(summary = "流水线-停用")
    public R<String> disable(@RequestBody PipelineDTO body) {
        return service.disable(body.getId()) ? R.success("停用成功") : R.fail("停用失败");
    }

    @GetMapping("/definition")
    @PreAuthorize("devops:pipeline:detail")
    @Operation(summary = "流水线-定义")
    public R<PipelineDefinitionVO> definition(@RequestParam("id") Long id,
                                              @RequestParam(value = "versionId", required = false) Long versionId) {
        return R.data(service.definition(id, versionId));
    }

    @PostMapping("/definition/validate")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线-定义校验")
    public R<ValidateResultVO> validateDefinition(@RequestBody java.util.Map<String, Object> body) {
        String definition = (String) body.get("definition");
        Integer mode = body.get("mode") == null ? null : Integer.valueOf(body.get("mode").toString());
        return R.data(service.validateDefinition(definition, mode));
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

    @PostMapping("/trigger/precheck")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线-触发预校验（五项）")
    public R<PrecheckResultVO> triggerPrecheck(@RequestBody @Valid RunTriggerDTO body) {
        return R.data(runService.precheck(body));
    }

    @PostMapping("/trigger")
    @PreAuthorize("devops:pipeline:trigger")
    @Operation(summary = "流水线-手动触发（幂等）")
    public R<TriggerResultVO> trigger(@RequestBody @Valid RunTriggerDTO body) {
        return R.data(runService.trigger(body));
    }

    @GetMapping("/trigger/records")
    @PreAuthorize("devops:pipeline:detail")
    @Operation(summary = "流水线-触发记录")
    public R<IPage<TriggerRecordVO>> triggerRecords(@RequestParam("appId") Long appId,
                                                    @RequestParam(value = "current", required = false) Long current,
                                                    @RequestParam(value = "size", required = false) Long size) {
        return R.data(service.triggerRecords(appId, current, size));
    }

}
