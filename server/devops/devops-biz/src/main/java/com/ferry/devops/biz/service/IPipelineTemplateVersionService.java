package com.ferry.devops.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import com.ferry.devops.api.vo.PipelineTemplateVersionVO;

import java.util.List;

public interface IPipelineTemplateVersionService extends IService<PipelineTemplateVersion> {

    /**
     * 流水线模板版本-列表
     *
     * @param templateId 模板id
     * @return 版本列表，按版本号倒序
     */
    List<PipelineTemplateVersionVO> list(Long templateId);

    /**
     * 流水线模板版本-详情
     *
     * @param id 版本id
     * @return 版本详情，含完整定义
     */
    PipelineTemplateVersionVO detail(Long id);

    /**
     * 流水线模板版本-保存定义
     *
     * <p>定义与当前版本一致时不生成新版本，否则版本号自增并生成不可变快照，
     * 同事务回写模板的阶段数与占位变量数。
     *
     * @param body 保存参数
     * @return true 成功 || false 失败
     */
    boolean save(PipelineTemplateVersionDTO body);

    /**
     * 流水线模板版本-回退
     *
     * @param body 回退参数
     * @return true 成功 || false 失败
     */
    boolean rollback(PipelineTemplateVersionDTO body);
}
