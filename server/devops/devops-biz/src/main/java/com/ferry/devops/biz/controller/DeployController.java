package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.DeployPrecheckDTO;
import com.ferry.devops.api.dto.DeployStartDTO;
import com.ferry.devops.api.dto.query.DeployQuery;
import com.ferry.devops.api.vo.DeployBatchVO;
import com.ferry.devops.api.vo.DeployDetailVO;
import com.ferry.devops.api.vo.DeployRecordVO;
import com.ferry.devops.api.vo.DeployStartVO;
import com.ferry.devops.api.vo.HealthSampleVO;
import com.ferry.devops.api.vo.PrecheckTokenVO;
import com.ferry.devops.biz.service.IDeployService;
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
 * 部署执行控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/deploy")
@Tag(name = "部署执行", description = "部署校验、灰度批次、健康检查与熔断")
public class DeployController {

    private final IDeployService service;

    @PostMapping("/precheck")
    @PreAuthorize("devops:deploy:precheck")
    @Operation(summary = "部署前五项校验")
    public R<PrecheckTokenVO> precheck(@RequestBody @Valid DeployPrecheckDTO body) {
        return R.data(service.precheck(body));
    }

    @PostMapping("/start")
    @PreAuthorize("devops:deploy:start")
    @Operation(summary = "启动部署")
    public R<DeployStartVO> start(@RequestBody @Valid DeployStartDTO body) {
        return R.data(service.start(body));
    }

    @PostMapping("/pageList")
    @PreAuthorize("devops:deploy:pageList")
    @Operation(summary = "部署记录分页列表")
    public R<IPage<DeployRecordVO>> pageList(@RequestBody DeployQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("devops:deploy:detail")
    @Operation(summary = "部署详情", parameters = @Parameter(name = "id", description = "部署id", required = true))
    public R<DeployDetailVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @GetMapping("/batches")
    @PreAuthorize("devops:deploy:detail")
    @Operation(summary = "灰度批次列表")
    public R<List<DeployBatchVO>> batches(@RequestParam("id") Long id) {
        return R.data(service.batches(id));
    }

    @PostMapping("/pause")
    @PreAuthorize("devops:deploy:pause")
    @Operation(summary = "暂停部署")
    public R<String> pause(@RequestBody Map<String, Object> body) {
        return service.pause(Long.valueOf(String.valueOf(body.get("id")))) ? R.success("暂停成功") : R.fail("暂停失败");
    }

    @PostMapping("/resume")
    @PreAuthorize("devops:deploy:resume")
    @Operation(summary = "恢复部署")
    public R<String> resume(@RequestBody Map<String, Object> body) {
        return service.resume(Long.valueOf(String.valueOf(body.get("id")))) ? R.success("恢复成功") : R.fail("恢复失败");
    }

    @GetMapping("/health")
    @PreAuthorize("devops:deploy:detail")
    @Operation(summary = "健康采样曲线")
    public R<List<HealthSampleVO>> health(@RequestParam("id") Long deployId,
                                          @RequestParam(value = "batchId", required = false) Long batchId) {
        return R.data(service.health(deployId, batchId));
    }

    @PostMapping("/circuit/continue")
    @PreAuthorize("devops:deploy:circuitContinue")
    @Operation(summary = "熔断后强制继续")
    public R<String> circuitContinue(@RequestBody Map<String, Object> body) {
        Long deployId = Long.valueOf(String.valueOf(body.get("deployId")));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        return service.circuitContinue(deployId, reason) ? R.success("已强制继续") : R.fail("操作失败");
    }

    /** 健康采样上报（供前端/内部决策写入） */
    @PostMapping("/health/save")
    @PreAuthorize("devops:deploy:detail")
    @Operation(summary = "写入健康采样")
    public R<String> saveHealth(@RequestBody Map<String, Object> body) {
        Long deployId = Long.valueOf(String.valueOf(body.get("deployId")));
        Long batchId = body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId")));
        Integer checkType = Integer.valueOf(String.valueOf(body.get("checkType")));
        Integer conclusion = Integer.valueOf(String.valueOf(body.get("conclusion")));
        boolean ok = service.saveHealthSample(deployId, batchId, checkType,
                String.valueOf(body.getOrDefault("value", "")),
                String.valueOf(body.getOrDefault("threshold", "")), conclusion);
        return ok ? R.success("保存成功") : R.fail("保存失败");
    }
}
