package com.ferry.devops.biz.service;

import java.util.List;

import com.ferry.devops.api.entity.RunStageGate;
import com.ferry.devops.api.vo.RunStageGateVO;

/**
 * 运行阶段门禁结果服务：结果展示、规则优先级判定与阻塞汇总。
 */
public interface IRunStageGateService {

    /** 运行下结果列表，不通过与数据获取失败置顶 */
    List<RunStageGateVO> listByRun(Long runId);

    /** 汇总运行门禁状态：存在不通过/数据获取失败则阻塞 */
    boolean isRunBlocked(Long runId);

    /** 是否存在适用于指定阶段/规则类型的启用规则 */
    boolean hasApplicableRule(Long appId, String branch, Integer stageType, Integer ruleType);

    /**
     * 门禁判定：按 分支>应用>组织 加载规则，比对当前值并写运行阶段门禁结果。
     *
     * @param runId         运行id
     * @param runStageId    运行阶段id
     * @param appId         应用id
     * @param branch        分支
     * @param stageType     阶段类型 1-8
     * @param ruleType      规则类型 1-6
     * @param currentValue  当前值
     * @param source        报告来源
     * @param schemaVersion 报告schema版本
     * @return 写入的结果
     */
    RunStageGate judge(Long runId, Long runStageId, Long appId, String branch, Integer stageType,
                       Integer ruleType, String currentValue, String source, String schemaVersion);

    /**
     * 数据获取失败判定：报告缺失/schema 不完整/拉取失败时写数据获取失败状态。
     */
    RunStageGate judgeFetchFailed(Long runId, Long runStageId, Long appId, String branch, Integer stageType,
                                  Integer ruleType, String source, String schemaVersion, String fetchError);

    /** 兼容旧调用：不按阶段类型过滤规则 */
    default RunStageGate judge(Long runId, Long runStageId, Long appId, String branch, Integer ruleType,
                               String currentValue, String source, String schemaVersion) {
        return judge(runId, runStageId, appId, branch, null, ruleType, currentValue, source, schemaVersion);
    }

    /** 兼容旧调用：不按阶段类型过滤规则 */
    default RunStageGate judgeFetchFailed(Long runId, Long runStageId, Long appId, String branch,
                                          Integer ruleType, String source, String schemaVersion, String fetchError) {
        return judgeFetchFailed(runId, runStageId, appId, branch, null, ruleType, source, schemaVersion, fetchError);
    }
}
