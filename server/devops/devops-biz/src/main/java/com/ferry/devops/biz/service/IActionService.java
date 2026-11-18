package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.ActionExecuteDTO;
import com.ferry.devops.api.dto.ActionProposeDTO;
import com.ferry.devops.api.dto.query.ActionRecordQuery;
import com.ferry.devops.api.entity.ActionRecord;
import com.ferry.devops.api.vo.ActionCatalogVO;
import com.ferry.devops.api.vo.ActionRecordVO;

import java.util.List;

/**
 * AI 动作引擎服务
 */
public interface IActionService extends IService<ActionRecord> {

    /** 启用的动作目录 */
    List<ActionCatalogVO> catalog(Integer level);

    /** 提议动作(仅记录，待执行) */
    ActionRecordVO propose(ActionProposeDTO body);

    /** 执行动作(L1 直接执行，L2 转待确认) */
    ActionRecordVO execute(ActionExecuteDTO body);

    /** 确认并执行 L2 动作 */
    ActionRecordVO confirm(ActionExecuteDTO body);

    /** 撤销动作(10 分钟窗口内) */
    boolean undo(Long recordId);

    /** 动作记录分页 */
    IPage<ActionRecordVO> records(ActionRecordQuery query);
}
