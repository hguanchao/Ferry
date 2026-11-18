package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.PrecheckResultVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunTaskVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.api.vo.RunVO;

import java.util.List;

/**
 * 运行服务
 */
public interface IRunService extends IService<Run> {

    IPage<RunVO> pageList(RunQuery body);

    RunDetailVO detail(Long id);

    List<RunStageVO> stages(Long runId);

    List<RunTaskVO> tasks(Long stageId);

    /** 日志分段（脱敏 + 截断），返回分段 VO 列表 */
    List<?> log(Long taskId, Integer fromLine, Integer limit);

    /** 日志关键词搜索，返回命中行 */
    List<?> logSearch(Long taskId, String keyword, Integer fromLine);

    /**
     * 写入日志（脱敏 + 500 行分段 + 超 10 万行截断），供 Jenkins 日志回调调用。
     */
    void saveLog(Long runId, Long taskId, String rawLog);

    PrecheckResultVO precheck(RunTriggerDTO body);

    TriggerResultVO trigger(RunTriggerDTO body);

    boolean pause(Long id, String reason);

    boolean resume(Long id);

    boolean abort(Long id, String reason);

    List<RunStageVO> rerunPlan(Long id);

    TriggerResultVO rerun(RunTriggerDTO body);
}
