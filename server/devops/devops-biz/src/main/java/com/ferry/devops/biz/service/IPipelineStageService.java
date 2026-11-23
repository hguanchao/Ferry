package com.ferry.devops.biz.service;

import java.util.List;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.entity.PipelineStage;
import com.ferry.devops.api.vo.PipelineStageVO;

/**
 * 流水线阶段服务：负责流水线定义在阶段表与任务表中的存取。
 *
 * <p>阶段服务负责阶段行，任务行委托 {@link IPipelineStageTaskService} 处理。
 */
public interface IPipelineStageService extends IService<PipelineStage> {

   List<PipelineStageVO> list(PipelineStageDTO body);

    /**
     * 保存一个版本的定义：写入阶段及阶段下的任务。
     *
     * @param pipelineId 流水线 id
     * @param version    版本号，调用方需保证是新版本
     * @param definition 结构化定义
     */
    void saveDefinition(Long pipelineId, Integer version, PipelineDefinitionDTO definition);

    /**
     * 读取指定版本的定义。
     *
     * @param pipelineId 流水线 id
     * @param version    版本号
     * @return 结构化定义，阶段列表为空时返回空列表
     */
    PipelineDefinitionDTO loadDefinition(Long pipelineId, Integer version);

    /**
     * 生成指定版本定义的 JSON 文本。
     *
     * @param pipelineId 流水线 id
     * @param version    版本号
     * @return 定义 JSON
     */
    String loadDefinitionJson(Long pipelineId, Integer version);

    /**
     * 查询流水线已有的版本号列表，按版本号降序。
     *
     * @param pipelineId 流水线 id
     * @return 版本号列表
     */
    List<Integer> listVersions(Long pipelineId);

    /**
     * 删除流水线全部版本的定义行。
     *
     * @param pipelineId 流水线 id
     */
    void deleteByPipeline(Long pipelineId);

    /**
     * 保存一个版本的阶段与任务，不负责 Jenkins 同步。
     *
     * @param bodys 阶段定义列表，空列表直接返回
     * @return 是否全部写入成功
     */
    boolean save(List<PipelineStageDTO> bodys);

    boolean deleted(List<Long> pipelineIds);
}
