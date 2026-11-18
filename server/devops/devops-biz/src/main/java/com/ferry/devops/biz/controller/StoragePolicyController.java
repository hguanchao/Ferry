package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.http.R;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.entity.StoragePolicy;
import com.ferry.devops.biz.mapper.StoragePolicyMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 存储与保留策略接口
 *
 * <p>前端以 scope/retentionDays 语义展示，落库映射到 app_id/log_keep_days。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/system/storagePolicy")
@Tag(name = "存储策略", description = "存储与保留策略")
public class StoragePolicyController {
    private final StoragePolicyMapper mapper;

    @PostMapping("/pageList")
    @PreAuthorize("devops:storage:pageList")
    @Operation(summary = "存储策略-分页列表")
    public R<IPage<Map<String, Object>>> pageList(@RequestBody Map<String, Object> body) {
        com.ferry.common.jdbc.provider.Query query = new com.ferry.common.jdbc.provider.Query();
        if (body.get("current") != null) {
            query.setCurrent(((Number) body.get("current")).intValue());
        }
        if (body.get("size") != null) {
            query.setSize(((Number) body.get("size")).intValue());
        }
        IPage<StoragePolicy> pages = mapper.selectPage(Page.create(query),
                Wrappers.<StoragePolicy>lambdaQuery().orderByDesc(StoragePolicy::getCreateTime));
        IPage<Map<String, Object>> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(pages.getRecords().stream().map(StoragePolicyController::toMap).collect(Collectors.toList()));
        return R.data(result);
    }

    @PostMapping("/save")
    @PreAuthorize("devops:storage:save")
    @Operation(summary = "存储策略-保存")
    public R<String> save(@RequestBody Map<String, Object> body) {
        StoragePolicy policy = new StoragePolicy();
        Object id = body.get("id");
        if (id != null) {
            policy.setId(((Number) id).longValue());
        }
        policy.setAppId("global".equals(body.get("scope")) ? 0L : 1L);
        policy.setLogKeepDays(body.get("retentionDays") == null ? 365 : ((Number) body.get("retentionDays")).intValue());
        policy.setArchivePath(body.get("description") == null ? null : String.valueOf(body.get("description")));
        if (policy.getId() == null) {
            mapper.insert(policy);
        } else {
            mapper.updateById(policy);
        }
        return R.success("保存成功");
    }

    /** 实体转前端 map，maxSizeGb 用归档路径占位，无独立列 */
    private static Map<String, Object> toMap(StoragePolicy policy) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(policy.getId()));
        map.put("scope", Long.valueOf(0L).equals(policy.getAppId()) ? "global" : "app");
        map.put("retentionDays", policy.getLogKeepDays());
        map.put("maxSizeGb", 0);
        map.put("description", policy.getArchivePath());
        map.put("createTime", policy.getCreateTime());
        return map;
    }
}
