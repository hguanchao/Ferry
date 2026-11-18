package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.ActionRecordQuery;
import com.ferry.devops.api.entity.ActionRecord;
import com.ferry.devops.api.vo.ActionRecordVO;

/**
 * 动作记录查询与VO转换
 */
public class ActionRecordWrapper extends BaseEntityWrapper<ActionRecord, ActionRecordVO> {

    public static ActionRecordWrapper build() {
        return new ActionRecordWrapper();
    }

    @Override
    public ActionRecordVO vo(ActionRecord entity) {
        return BeanUtil.copy(entity, ActionRecordVO.class);
    }

    public LambdaQueryWrapper<ActionRecord> lambda(ActionRecordQuery query) {
        return Wrappers.<ActionRecord>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getActionId()), ActionRecord::getActionId, query.getActionId())
                .eq(ObjUtil.isNotEmpty(query.getAppId()), ActionRecord::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getLevel()), ActionRecord::getLevel, query.getLevel())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), ActionRecord::getStatus, query.getStatus())
                .eq(ObjUtil.isNotEmpty(query.getSource()), ActionRecord::getSource, query.getSource())
                .orderByDesc(ActionRecord::getCreateTime);
    }
}
