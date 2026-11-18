package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ferry.common.core.http.R;
import com.ferry.common.jdbc.provider.Query;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.NetworkClosureDTO;
import com.ferry.devops.api.vo.NetworkClosureVO;
import com.ferry.devops.biz.service.IReleaseStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 封网期控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/networkClosure")
@Tag(name = "封网期", description = "全局/应用/团队封网窗口维护")
public class NetworkClosureController {

    private final IReleaseStrategyService service;

    /** 前端分页：接收 Query 格式 {current,size} */
    @PostMapping("/pageList")
    @PreAuthorize("devops:releaseStrategy:closurePageList")
    @Operation(summary = "封网期分页列表")
    public R<IPage<NetworkClosureVO>> pageList(@RequestBody(required = false) Map<String, Object> body) {
        Query query = new Query();
        if (body != null) {
            if (body.get("current") != null) {
                query.setCurrent(Integer.valueOf(String.valueOf(body.get("current"))));
            }
            if (body.get("size") != null) {
                query.setSize(Integer.valueOf(String.valueOf(body.get("size"))));
            }
        }
        List<NetworkClosureVO> all = service.closureList();
        int current = query.getCurrent() == null ? 1 : query.getCurrent();
        int size = query.getSize() == null ? 10 : query.getSize();
        int from = Math.min((current - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        IPage<NetworkClosureVO> page = new Page<>(current, size, all.size());
        page.setRecords(all.subList(from, to));
        return R.data(page);
    }

    @PostMapping("/save")
    @PreAuthorize("devops:releaseStrategy:closureSave")
    @Operation(summary = "保存封网期")
    public R<String> save(@RequestBody @Valid NetworkClosureDTO body) {
        return service.saveClosure(body) ? R.success("保存成功") : R.fail("保存失败");
    }
}
