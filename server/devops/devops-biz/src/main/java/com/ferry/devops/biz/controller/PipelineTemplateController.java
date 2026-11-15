package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineTemplateDTO;
import com.ferry.devops.api.dto.query.PipelineTemplateQuery;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.api.vo.PipelineVO;
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
@RequestMapping("/pipeline/template")
@Tag(name = "流水线模板", description = "流水线模板API接口控制器")
public class PipelineTemplateController {
    private final IPipelineTemplateService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:template:pageList")
    @Operation(summary = "流水线模板-分页列表", description = "按层级分页查询，层级必传")
    public R<IPage<PipelineTemplateVO>> pageList(@RequestBody PipelineTemplateQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:template:detail")
    @Operation(summary = "流水线模板-详情", description = "传入模板id", parameters = {
            @Parameter(name = "id", description = "模板id", required = true)
    })
    public R<PipelineTemplateVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:template:save")
    @Operation(summary = "流水线模板-新增或编辑", description = "新增时携带初始定义并生成 V1，编辑只改名称与适用场景")
    public R<String> save(@RequestBody @Valid PipelineTemplateDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:template:deleted")
    @Operation(summary = "流水线模板-逻辑删除", description = "传入模板id(支持批量)，已有派生流水线的模板只能停用")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/status")
    @PreAuthorize("devops:template:status")
    @Operation(summary = "流水线模板-停用或启用", description = "停用后不可被新流水线选用，已派生流水线不受影响", parameters = {
            @Parameter(name = "id", description = "模板id", required = true),
            @Parameter(name = "status", description = "状态 0：停用 1：启用", required = true)
    })
    public R<String> changeStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        boolean isSuccess = service.changeStatus(id, status);
        return isSuccess ? R.success("修改成功") : R.fail("修改失败");
    }

    @PostMapping("/derivedList")
    @PreAuthorize("devops:template:derivedList")
    @Operation(summary = "流水线模板-派生流水线清单", description = "传入模板id，展示由该模板派生的流水线与落后状态")
    public R<IPage<PipelineVO>> derivedList(@RequestBody PipelineTemplateQuery query) {
        return R.data(service.derivedList(query));
    }

    @GetMapping("/options")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线模板-选用候选", description = "创建流水线时可选用的启用模板，组织级与团队级合并返回", parameters = {
            @Parameter(name = "keyword", description = "模板名称模糊搜索")
    })
    public R<List<PipelineTemplateVO>> options(@RequestParam(value = "keyword", required = false) String keyword) {
        return R.data(service.options(keyword));
    }
}
