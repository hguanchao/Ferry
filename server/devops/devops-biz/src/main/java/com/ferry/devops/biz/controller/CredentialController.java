package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.CredentialDTO;
import com.ferry.devops.api.dto.query.CredentialQuery;
import com.ferry.devops.api.vo.CredentialVO;
import com.ferry.devops.biz.service.ICredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/credential")
@Tag(name = "凭据管理", description = "凭据管理API接口控制器")
public class CredentialController {
    private final ICredentialService service;

    @PostMapping("/pageList")
    @PreAuthorize("devops:credential:pageList")
    @Operation(summary = "凭据管理-分页列表（仅掩码）")
    public R<IPage<CredentialVO>> pageList(@RequestBody CredentialQuery body) {
        return R.data(service.pageList(body));
    }

    @PostMapping("/save")
    @PreAuthorize("devops:credential:save")
    @Operation(summary = "凭据管理-新增或编辑")
    public R<String> save(@RequestBody @Valid CredentialDTO body) {
        boolean ok = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return ok ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/rotate/{id}")
    @PreAuthorize("devops:credential:rotate")
    @Operation(summary = "凭据管理-轮换")
    public R<String> rotate(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
        return service.rotate(id, value) ? R.success("轮换成功") : R.fail("轮换失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("devops:credential:deleted")
    @Operation(summary = "凭据管理-删除（被引用不可删）")
    public R<String> deleted(@RequestBody List<Long> ids) {
        return service.deleted(ids) ? R.success("删除成功") : R.fail("删除失败");
    }
}
