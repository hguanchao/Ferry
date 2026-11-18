package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.ActionExecuteDTO;
import com.ferry.devops.api.dto.ActionProposeDTO;
import com.ferry.devops.api.dto.query.ActionRecordQuery;
import com.ferry.devops.api.entity.ActionCatalog;
import com.ferry.devops.api.entity.ActionRecord;
import com.ferry.devops.api.entity.AppAiLevel;
import com.ferry.devops.api.enums.ActionLevelEnum;
import com.ferry.devops.api.enums.ActionStatusEnum;
import com.ferry.devops.api.vo.ActionCatalogVO;
import com.ferry.devops.api.vo.ActionRecordVO;
import com.ferry.devops.biz.mapper.ActionCatalogMapper;
import com.ferry.devops.biz.mapper.ActionRecordMapper;
import com.ferry.devops.biz.mapper.AppAiLevelMapper;
import com.ferry.devops.biz.service.IActionService;
import com.ferry.devops.biz.wrapper.ActionRecordWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * AI 动作引擎服务实现
 */
@Service
@RequiredArgsConstructor
public class ActionServiceImpl extends ServiceImpl<ActionRecordMapper, ActionRecord> implements IActionService {
    private final ActionCatalogMapper catalogMapper;
    private final AppAiLevelMapper appAiLevelMapper;

    @Override
    public List<ActionCatalogVO> catalog(Integer level) {
        List<ActionCatalog> list = catalogMapper.selectList(Wrappers.<ActionCatalog>lambdaQuery()
                .eq(ActionCatalog::getIsEnabled, IntConst.ONE)
                .eq(ObjUtil.isNotEmpty(level), ActionCatalog::getLevel, level)
                .orderByAsc(ActionCatalog::getLevel));
        return BeanUtil.copy(list, ActionCatalogVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionRecordVO propose(ActionProposeDTO body) {
        ActionCatalog catalog = requireCatalog(body.getActionId());
        // 前置条件非空
        if (StrUtil.isBlank(catalog.getPrecondition())) {
            throw new ServiceException("动作未配置前置条件，不可提议");
        }
        ActionRecord record = new ActionRecord();
        record.setActionId(body.getActionId());
        record.setLevel(catalog.getLevel());
        record.setSource(body.getSource());
        record.setBizId(body.getBizId());
        record.setAppId(body.getAppId());
        record.setEnvId(body.getEnvId());
        record.setParamsJson(body.getParams());
        record.setReason(body.getReason());
        record.setEvidenceJson(JsonUtil.toJson(body.getEvidence()));
        record.setOperator(AuthUtil.getUserId());
        record.setModelVersion("min-v1");
        record.setPromptVersion("p-v1");
        record.setStatus(ActionStatusEnum.PROPOSED.getCode());
        baseMapper.insert(record);
        return BeanUtil.copy(record, ActionRecordVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionRecordVO execute(ActionExecuteDTO body) {
        ActionRecord record = requireRecord(body.getRecordId());
        ActionCatalog catalog = requireCatalog(record.getActionId());
        // 六项校验中的关键四项：目录启用 + 前置条件 + 频控(简化跳过) + 级别
        checkEnabled(catalog);
        checkPrecondition(catalog);
        int level = catalog.getLevel();
        checkAuthorized(record.getAppId(), level);
        if (ObjUtil.equals(level, ActionLevelEnum.L3.getCode())) {
            record.setStatus(ActionStatusEnum.REJECTED.getCode());
            record.setError("L3 动作禁止自动执行，需人工走变更流程");
            baseMapper.updateById(record);
            throw new ServiceException("该操作需人工走变更流程");
        }
        if (ObjUtil.equals(level, ActionLevelEnum.L2.getCode())) {
            record.setStatus(ActionStatusEnum.PENDING_CONFIRM.getCode());
            baseMapper.updateById(record);
            return BeanUtil.copy(record, ActionRecordVO.class);
        }
        // L1 直接执行
        return doExecute(record, catalog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionRecordVO confirm(ActionExecuteDTO body) {
        ActionRecord record = requireRecord(body.getRecordId());
        if (!ObjUtil.equals(record.getStatus(), ActionStatusEnum.PENDING_CONFIRM.getCode())) {
            throw new ServiceException("动作不处于待确认状态");
        }
        if (!Boolean.TRUE.equals(body.getImpactConfirmed())) {
            throw new ServiceException("请先勾选已确认影响面");
        }
        ActionCatalog catalog = requireCatalog(record.getActionId());
        checkAuthorized(record.getAppId(), catalog.getLevel());
        return doExecute(record, catalog);
    }

    /** L1/L2 落地执行：置成功、写执行时间，可撤销 L1 写 10 分钟撤销截止 */
    private ActionRecordVO doExecute(ActionRecord record, ActionCatalog catalog) {
        record.setStatus(ActionStatusEnum.SUCCESS.getCode());
        record.setExecuteTime(new Date());
        record.setResult("执行成功");
        if (ObjUtil.equals(catalog.getLevel(), ActionLevelEnum.L1.getCode())
                && ObjUtil.equals(catalog.getIsUndoable(), IntConst.ONE)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 10);
            record.setUndoDeadline(cal.getTime());
        }
        baseMapper.updateById(record);
        return BeanUtil.copy(record, ActionRecordVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean undo(Long recordId) {
        ActionRecord record = requireRecord(recordId);
        if (record.getUndoDeadline() == null || record.getUndoDeadline().before(new Date())) {
            throw new ServiceException("撤销窗口已过，请走人工流程");
        }
        if (!ObjUtil.equals(record.getStatus(), ActionStatusEnum.SUCCESS.getCode())) {
            throw new ServiceException("仅执行成功的动作可撤销");
        }
        record.setStatus(ActionStatusEnum.REVOKED.getCode());
        record.setUndoTime(new Date());
        record.setUndoUser(AuthUtil.getUserId());
        return baseMapper.updateById(record) > IntConst.ZERO;
    }

    @Override
    public IPage<ActionRecordVO> records(ActionRecordQuery query) {
        ActionRecordWrapper wrapper = ActionRecordWrapper.build();
        return wrapper.pageVo(page(Page.create(query), wrapper.lambda(query)));
    }

    private ActionCatalog requireCatalog(String actionId) {
        ActionCatalog catalog = catalogMapper.selectOne(Wrappers.<ActionCatalog>lambdaQuery()
                .eq(ActionCatalog::getActionId, actionId));
        if (catalog == null) {
            throw new ServiceException("动作不在目录白名单内");
        }
        return catalog;
    }

    private ActionRecord requireRecord(Long id) {
        ActionRecord record = baseMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("动作记录不存在");
        }
        return record;
    }

    private void checkEnabled(ActionCatalog catalog) {
        if (!ObjUtil.equals(catalog.getIsEnabled(), IntConst.ONE)) {
            throw new ServiceException("动作已停用");
        }
    }

    private void checkPrecondition(ActionCatalog catalog) {
        if (StrUtil.isBlank(catalog.getPrecondition())) {
            throw new ServiceException("前置条件未满足");
        }
    }

    /** 授权级别校验：应用默认 L0，L2 需逐应用授权 */
    private void checkAuthorized(Long appId, int level) {
        if (ObjUtil.equals(level, ActionLevelEnum.L0.getCode())) {
            return;
        }
        AppAiLevel granted = appAiLevelMapper.selectOne(Wrappers.<AppAiLevel>lambdaQuery()
                .eq(AppAiLevel::getAppId, appId));
        int grantedLevel = granted == null ? ActionLevelEnum.L0.getCode() : granted.getLevel();
        if (level > grantedLevel) {
            throw new ServiceException("应用未开通该级别动作权限");
        }
    }
}
