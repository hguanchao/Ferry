package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.RunOptionDTO;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.dto.query.RunTriggerRecordQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.CommitBriefVO;
import com.ferry.devops.api.vo.PrecheckItemVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import java.util.List;

public interface IRunService extends IService<Run> {

    /**
     * 手动触发-分支候选
     *
     * <p>实时查询代码平台分支列表，不做缓存。
     *
     * @param body 查询参数
     * @return 分支名列表
     */
    List<String> branchOptions(RunOptionDTO body);

    /**
     * 手动触发-提交候选
     *
     * @param body 查询参数
     * @return 最近提交摘要列表
     */
    List<CommitBriefVO> commitOptions(RunOptionDTO body);

    /**
     * 手动触发-五项预校验
     *
     * <p>逐条返回结论但不落库；触发时服务端会重跑同一套校验并随运行落库。
     *
     * @param body 触发参数
     * @return 预校验结论列表
     */
    List<PrecheckItemVO> precheck(RunTriggerDTO body);

    /**
     * 手动触发-发起运行
     *
     * <p>以（应用、commit、流水线版本）为幂等键：命中进行中运行返回其 run_id 并标记「已合并」；
     * 命中成功运行时先返回待确认复用，用户确认后带 reuse 重新触发并标记「复用运行」；
     * 五项预校验任一不通过则返回「已拒绝」及原因，不产生运行。
     *
     * @param body 触发参数
     * @return 触发结果
     */
    TriggerResultVO trigger(RunTriggerDTO body);

    /**
     * 手动触发-触发记录分页
     *
     * <p>包含被拒绝的触发，作为触发动作的审计载体。
     *
     * @param body 查询条件
     * @return 触发记录分页
     */
    IPage<TriggerRecordVO> triggerPageList(RunTriggerRecordQuery body);

    /**
     * 运行台账-分页列表
     *
     * @param body 查询条件
     * @return 运行分页列表
     */
    IPage<RunVO> pageList(RunQuery body);

    /**
     * 运行台账-详情
     *
     * <p>含五项预校验结论与阶段列表；各阶段任务明细来自运行绑定的版本快照。
     *
     * @param id 运行id
     * @return 运行详情
     */
    RunDetailVO detail(Long id);

    /**
     * 运行台账-取消
     *
     * <p>仅排队中的运行可以取消。
     *
     * @param id 运行id
     * @return true 成功 || false 失败
     */
    boolean cancel(Long id);
}
