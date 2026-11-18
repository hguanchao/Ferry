package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.OpenTokenDTO;
import com.ferry.devops.api.dto.query.OpenTokenQuery;
import com.ferry.devops.api.vo.OpenTokenVO;
import com.ferry.devops.biz.service.IOpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开放 API 控制器：Token 管理 + 8 个开放接口的简化实现。
 *
 * <p>鉴权与限流由网关 OpenApiAuthFilter 负责，本控制器只返回固定结构数据；
 * 业务层校验资源归属由网关注入的 X-Open-App-Id 完成(最小实现中省略)。
 */
@RestController
@AllArgsConstructor
@Tag(name = "开放 API", description = "Token 管理与开放接口")
public class OpenApiController {
    private final IOpenApiService service;

    @PostMapping("/system/openToken/pageList")
    @PreAuthorize("system:openApi:pageList")
    @Operation(summary = "Token 分页")
    public R<IPage<OpenTokenVO>> tokenPage(@RequestBody OpenTokenQuery body) {
        return R.data(service.tokenPage(body));
    }

    @PostMapping("/system/openToken/issue")
    @PreAuthorize("system:openApi:issue")
    @Operation(summary = "签发 Token")
    public R<String> issue(@RequestBody @Valid OpenTokenDTO body) {
        return R.data(service.issue(body).getTokenPlain(), "签发成功，请妥善保存明文");
    }

    @PostMapping("/system/openToken/rotate/{id}")
    @PreAuthorize("system:openApi:rotate")
    @Operation(summary = "轮换 Token")
    public R<String> rotate(@PathVariable("id") Long id) {
        return R.data(service.rotate(id).getTokenPlain(), "轮换成功，请妥善保存明文");
    }

    @PostMapping("/system/openToken/disable/{id}")
    @PreAuthorize("system:openApi:disable")
    @Operation(summary = "禁用 Token")
    public R<String> disable(@PathVariable("id") Long id) {
        return service.disable(id) ? R.success("禁用成功") : R.fail("禁用失败");
    }

    // ---------- 8 个开放接口简化实现(鉴权由网关负责) ----------

    @GetMapping("/open/v1/runs/{runId}")
    @Operation(summary = "开放接口-查询运行状态")
    public R<Map<String, Object>> openRun(@PathVariable("runId") Long runId) {
        return R.data(openData("runId", runId));
    }

    @GetMapping("/open/v1/artifacts")
    @Operation(summary = "开放接口-查询制品")
    public R<Map<String, Object>> openArtifacts() {
        return R.data(openData("artifacts", "ok"));
    }

    @PostMapping("/open/v1/artifacts/{id}/promote")
    @Operation(summary = "开放接口-制品晋级")
    public R<Map<String, Object>> openPromote(@PathVariable("id") Long id) {
        return R.data(openData("promoted", id));
    }

    @GetMapping("/open/v1/environments")
    @Operation(summary = "开放接口-查询环境")
    public R<Map<String, Object>> openEnvironments() {
        return R.data(openData("environments", "ok"));
    }

    @PostMapping("/open/v1/release-candidates")
    @Operation(summary = "开放接口-创建发布单")
    public R<Map<String, Object>> openCreateRc(@RequestBody(required = false) Map<String, Object> body) {
        return R.data(openData("created", true));
    }

    @GetMapping("/open/v1/release-candidates/{id}")
    @Operation(summary = "开放接口-查询发布单")
    public R<Map<String, Object>> openRc(@PathVariable("id") Long id) {
        return R.data(openData("rcId", id));
    }

    @PostMapping("/open/v1/rollbacks")
    @Operation(summary = "开放接口-发起回滚")
    public R<Map<String, Object>> openRollback(@RequestBody(required = false) Map<String, Object> body) {
        return R.data(openData("rollback", "accepted"));
    }

    @GetMapping("/open/v1/health-checks")
    @Operation(summary = "开放接口-查询健康检查")
    public R<Map<String, Object>> openHealth() {
        return R.data(openData("health", "ok"));
    }

    private Map<String, Object> openData(String key, Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(key, value);
        return data;
    }
}
