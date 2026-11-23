package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.VersionVO;

import java.util.List;

/**
 * 流水线服务：保存后经由同步编排创建/更新 Jenkins Job（保存即同步），
 * 触发侧凭流水线状态拦截未同步脚本。
 */
public interface IPipelineService extends IService<Pipeline> {

    IPage<PipelineVO> pageList(PipelineQuery body);

    PipelineVO detail(Long id);

    boolean save(PipelineDTO body);

    boolean deleted(List<Long> ids);

    boolean changeStatus(Long id, Integer status);

    /**
     * 回退到指定历史版本（复制为新版本）：供断点重跑恢复运行时脚本快照。
     */
    VersionVO rollback(Long id, Long versionId, String changeNote);
}
