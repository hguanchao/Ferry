package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.NotifyChannelDTO;
import com.ferry.devops.api.dto.NotifyTemplateDTO;
import com.ferry.devops.api.dto.query.NotifyChannelQuery;
import com.ferry.devops.api.dto.query.NotifyTemplateQuery;
import com.ferry.devops.api.vo.NotifyChannelVO;
import com.ferry.devops.api.vo.NotifyTemplateVO;
import com.ferry.devops.biz.service.INotifyService;
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

/**
 * 通知渠道与模板控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/system")
@Tag(name = "通知管理", description = "通知渠道与模板配置")
public class NotifyController {
    private final INotifyService service;

    @PostMapping("/notifyChannel/pageList")
    @PreAuthorize("system:notify:pageList")
    @Operation(summary = "通知渠道分页")
    public R<IPage<NotifyChannelVO>> channelPage(@RequestBody NotifyChannelQuery body) {
        return R.data(service.channelPage(body));
    }

    @PostMapping("/notifyChannel/save")
    @PreAuthorize("system:notify:save")
    @Operation(summary = "通知渠道新增或编辑")
    public R<String> saveChannel(@RequestBody @Valid NotifyChannelDTO body) {
        return service.saveChannel(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/notifyChannel/test/{id}")
    @PreAuthorize("system:notify:test")
    @Operation(summary = "发送测试消息")
    public R<String> testChannel(@PathVariable("id") Long id) {
        return service.testChannel(id) ? R.success("测试消息已投递") : R.fail("测试失败");
    }

    @PostMapping("/notifyChannel/deleted")
    @PreAuthorize("system:notify:deleted")
    @Operation(summary = "删除通知渠道")
    public R<String> deletedChannels(@RequestBody List<Long> ids) {
        return service.deletedChannels(ids) ? R.success("删除成功") : R.fail("删除失败");
    }

    @PostMapping("/notifyTemplate/pageList")
    @PreAuthorize("system:notify:templatePageList")
    @Operation(summary = "通知模板分页")
    public R<IPage<NotifyTemplateVO>> templatePage(@RequestBody NotifyTemplateQuery body) {
        return R.data(service.templatePage(body));
    }

    @PostMapping("/notifyTemplate/save")
    @PreAuthorize("system:notify:templateSave")
    @Operation(summary = "通知模板新增或编辑")
    public R<String> saveTemplate(@RequestBody @Valid NotifyTemplateDTO body) {
        return service.saveTemplate(body) ? R.success("保存成功") : R.fail("保存失败");
    }

    @PostMapping("/notifyTemplate/deleted")
    @PreAuthorize("system:notify:templateSave")
    @Operation(summary = "删除通知模板")
    public R<String> deletedTemplates(@RequestBody List<Long> ids) {
        return service.deletedTemplates(ids) ? R.success("删除成功") : R.fail("删除失败");
    }
}
