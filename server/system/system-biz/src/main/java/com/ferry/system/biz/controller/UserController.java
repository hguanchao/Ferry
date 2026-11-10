package com.ferry.system.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.UserDTO;
import com.ferry.system.api.dto.query.UserQuery;
import com.ferry.system.api.vo.UserVO;
import com.ferry.system.biz.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户管理API接口控制器")
public class UserController {
    private final IUserService service;

    @PostMapping("/pageList")
    @PreAuthorize("system:user:pageList")
    @Operation(summary = "用户管理-分页列表", description = "传入查询条件")
    public R<IPage<UserVO>> pageList(@RequestBody UserQuery body) {
        return R.data(service.pageList(body));
    }

    @GetMapping("/detail")
    @PreAuthorize("system:user:detail")
    @Operation(summary = "用户管理-详情信息", description = "传入用户id", parameters = {
            @Parameter(name = "id", description = "用户id", required = true)
    })
    public R<UserVO> detail(@RequestParam("id") Long id) {
        return R.data(service.detail(id));
    }

    @PostMapping("/save")
    @PreAuthorize("system:user:save")
    @Operation(summary = "用户管理-编辑信息", description = "传入表单数据")
    public R<String> save(@RequestBody @Valid UserDTO body) {
        boolean isSuccess = service.save(body);
        String msg = ObjUtil.isEmpty(body.getId()) ? "新增" : "修改";
        return isSuccess ? R.success(msg + "成功") : R.fail(msg + "失败");
    }

    @PostMapping("/resetPassword/{id}")
    @PreAuthorize("system:user:resetPassword")
    @Operation(summary = "用户管理-重置密码", description = "恢复为系统默认密码，仅超管可执行")
    public R<String> resetPassword(@PathVariable("id") Long id) {
        boolean isSuccess = service.resetPassword(id);
        return isSuccess ? R.success("重置成功") : R.fail("重置失败");
    }

    @PostMapping("/deleted")
    @PreAuthorize("system:user:deleted")
    @Operation(summary = "用户管理-删除信息", description = "传入用户id(支持批量)")
    public R<String> deleted(@RequestBody List<Long> ids) {
        boolean isSuccess = service.deleted(ids);
        return isSuccess ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/authorizeRole")
    @PreAuthorize("system:user:authorizeRole")
    @Operation(summary = "用户管理-用户授权角色", description = "传入用户id和角色id")
    public R<String> authorizeRole(@RequestBody RoleAuthorize<Long, Long> body) {
        boolean isSuccess = service.authorizeRole(body);
        return isSuccess ? R.success("授权成功") : R.fail("授权失败");
    }
}
