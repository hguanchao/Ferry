package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.biz.service.IPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
@Tag(name = "流水线管理", description = "流水线基本信息API接口控制器")
public class PipelineController {
    private final IPipelineService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:pipeline:pageList")
    @Operation(summary = "流水线管理-分页列表", description = "传入查询条件")
    public R<IPage<PipelineVO>> pageList(@RequestBody PipelineQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:pipeline:detail")
    @Operation(summary = "流水线管理-详情", description = "传入流水线id", parameters = {
            @Parameter(name = "id", description = "流水线id", required = true)
    })
    public R<PipelineVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:pipeline:save")
    @Operation(summary = "流水线管理-新增或编辑", description = "新增时携带初始定义，编辑只改名称与描述")
    public R<String> save(@RequestBody @Valid PipelineDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:pipeline:deleted")
    @Operation(summary = "流水线管理-逻辑删除", description = "传入流水线id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/export")
    @PreAuthorize("devops:pipeline:export")
    @Operation(summary = "流水线管理-导出", description = "传查询条件与勾选id，返回 xlsx 文件流")
    public void export(@RequestBody PipelineQuery body, HttpServletResponse response) {
        service.export(body, response);
    }
}
