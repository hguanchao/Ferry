package com.ferry.devops.biz.service.impl;

import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.entity.RunStageTask;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.ferry.devops.biz.service.IRunStageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
public class RunStageTaskServiceImpl extends ServiceImpl<RunStageTaskMapper, RunStageTask> implements IRunStageTaskService {
}
