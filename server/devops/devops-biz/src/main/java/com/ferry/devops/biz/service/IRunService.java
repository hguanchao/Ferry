package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.api.vo.RunVO;

import java.util.List;

/**
 * 运行服务
 */
public interface IRunService extends IService<Run> {

    IPage<RunVO> pageList(RunQuery body);

    RunVO detail(Long id);

    TriggerResultVO trigger(RunTriggerDTO body);

    boolean pause(Long id, String reason);

    boolean resume(Long id);

    boolean abort(Long id, String reason);

    List<RunStageVO> rerunPlan(Long id);

    TriggerResultVO rerun(RunTriggerDTO body);
}
