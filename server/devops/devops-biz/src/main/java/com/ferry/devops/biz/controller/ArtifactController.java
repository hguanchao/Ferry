package com.ferry.devops.biz.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.ArtifactDTO;
import com.ferry.devops.api.dto.ArtifactPromoteDTO;
import com.ferry.devops.api.dto.query.ArtifactQuery;
import com.ferry.devops.api.vo.ArtifactDetailVO;
import com.ferry.devops.api.vo.ArtifactVO;
import com.ferry.devops.api.vo.PromotionPrecheckVO;
import com.ferry.devops.api.vo.TraceVO;
import com.ferry.devops.biz.service.IArtifactService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * 制品管理：列表追溯、入库复核、晋级、软删除恢复。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/artifact")
@Tag(name = "制品管理", description = "制品列表、追溯、入库、晋级与恢复")
public class ArtifactController {

    private final IArtifactService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:artifact:pageList")
    @Operation(summary = "制品-分页列表")
    public R<IPage<ArtifactVO>> pageList(@RequestBody ArtifactQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:artifact:detail")
    @Operation(summary = "制品-详情")
    public R<ArtifactDetailVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @GetMapping("/trace")
    @PreAuthorize("devops:artifact:trace")
    @Operation(summary = "制品-追溯")
    public R<TraceVO> trace(@RequestParam("id") Long id) {
        return R.data(service.trace(id));
    }

    @GetMapping("/search")
    @PreAuthorize("devops:artifact:search")
    @Operation(summary = "制品-摘要/版本/Commit 反查")
    public R<List<ArtifactVO>> search(@RequestParam("keyword") String keyword) {
        return R.data(service.search(keyword));
    }

    @PostMapping("/intake")
    @PreAuthorize("devops:artifact:intake")
    @Operation(summary = "制品-入库复核")
    public R<String> intake(@RequestBody @Valid ArtifactDTO body) {
        return service.intake(body) ? R.success("入库成功") : R.fail("入库失败");
    }

    @PostMapping("/promote/precheck")
    @PreAuthorize("devops:artifact:promote")
    @Operation(summary = "制品-晋级预检")
    public R<PromotionPrecheckVO> promotePrecheck(@RequestBody @Valid ArtifactPromoteDTO body) {
        return R.data(service.promotePrecheck(body.getId(), body.getTargetEnvId()));
    }

    @PostMapping("/promote")
    @PreAuthorize("devops:artifact:promote")
    @Operation(summary = "制品-晋级")
    public R<String> promote(@RequestBody @Valid ArtifactPromoteDTO body) {
        return service.promote(body) ? R.success("晋级成功") : R.fail("晋级失败");
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("devops:artifact:restore")
    @Operation(summary = "制品-软删除恢复")
    public R<String> restore(@PathVariable("id") Long id) {
        return service.restore(id) ? R.success("恢复成功") : R.fail("恢复失败");
    }
}
