package com.ferry.devops.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineVersionDTO;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineVersionVO;

import java.util.List;

public interface IPipelineVersionService extends IService<PipelineVersion> {

    /**
     * 流水线版本-列表
     *
     * @param pipelineId 流水线id
     * @return 版本列表，按版本号倒序
     */
    List<PipelineVersionVO> list(Long pipelineId);

    /**
     * 流水线版本-详情
     *
     * @param id 版本id
     * @return 版本详情，含完整定义
     */
    PipelineVersionVO detail(Long id);

    /**
     * 流水线版本-保存定义
     *
     * <p>定义与当前版本一致时不生成新版本，否则版本号自增并生成不可变快照。
     *
     * @param body 保存参数
     * @return true 成功 || false 失败
     */
    boolean save(PipelineVersionDTO body);

    /**
     * 流水线版本-回退
     *
     * @param body 回退参数
     * @return true 成功 || false 失败
     */
    boolean rollback(PipelineVersionDTO body);
}
