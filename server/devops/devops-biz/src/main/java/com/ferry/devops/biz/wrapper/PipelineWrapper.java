package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineTemplateDerivation;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateDerivationMapper;
import com.ferry.devops.biz.mapper.PipelineVersionMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流水线列表查询与VO转换
 */
public class PipelineWrapper extends BaseEntityWrapper<Pipeline, PipelineVO> {

    private final PipelineMapper mapper;
    private final PipelineVersionMapper versionMapper;
    private final PipelineTemplateDerivationMapper derivationMapper;

    private PipelineWrapper(PipelineMapper mapper, PipelineVersionMapper versionMapper,
                            PipelineTemplateDerivationMapper derivationMapper) {
        this.mapper = mapper;
        this.versionMapper = versionMapper;
        this.derivationMapper = derivationMapper;
    }

    public static PipelineWrapper build(PipelineMapper mapper, PipelineVersionMapper versionMapper,
                                        PipelineTemplateDerivationMapper derivationMapper) {
        return new PipelineWrapper(mapper, versionMapper, derivationMapper);
    }

    @Override
    public PipelineVO vo(Pipeline entity) {
        return BeanUtil.copy(entity, PipelineVO.class);
    }

    /**
     * 组装列表 VO：回填应用名称、当前版本号、派生落后标记
     */
    public List<PipelineVO> voWithExtras(List<Pipeline> list) {
        List<PipelineVO> result = listVo(list);
        Map<Long, PipelineVersion> versionMap = versionMap(list);
        Map<Long, PipelineTemplateDerivation> derivationMap = derivationMap(list);
        for (PipelineVO vo : result) {
            vo.setAppName(mapper.selectAppName(vo.getAppId()));
            PipelineVersion version = versionMap.get(vo.getId());
            if (version != null) {
                vo.setCurrentVersionNo(version.getVersionNo());
            }
            PipelineTemplateDerivation derivation = derivationMap.get(vo.getId());
            if (derivation != null) {
                vo.setTemplateId(derivation.getTemplateId());
                vo.setIsBehind(derivation.getIsBehind());
            }
        }
        return result;
    }

    private Map<Long, PipelineVersion> versionMap(List<Pipeline> list) {
        List<Long> ids = list.stream().map(Pipeline::getCurrentVersionId).filter(ObjUtil::isNotEmpty).collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            return java.util.Collections.emptyMap();
        }
        List<PipelineVersion> versions = versionMapper.selectBatchIds(ids);
        return versions.stream().collect(Collectors.toMap(PipelineVersion::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, PipelineTemplateDerivation> derivationMap(List<Pipeline> list) {
        List<Long> ids = list.stream().map(Pipeline::getId).collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            return java.util.Collections.emptyMap();
        }
        List<PipelineTemplateDerivation> rows = derivationMapper.selectList(Wrappers.<PipelineTemplateDerivation>lambdaQuery()
                .in(PipelineTemplateDerivation::getPipelineId, ids));
        return rows.stream().collect(Collectors.toMap(PipelineTemplateDerivation::getPipelineId, Function.identity(), (a, b) -> a));
    }

    /**
     * 按流水线列表筛选条件构造查询
     */
    public LambdaQueryWrapper<Pipeline> lambda(PipelineQuery query) {
        return Wrappers.<Pipeline>lambdaQuery()
                .in(CollUtil.isNotEmpty(query.getAppIds()), Pipeline::getAppId, query.getAppIds())
                .like(StrUtil.isNotBlank(query.getName()), Pipeline::getName, query.getName())
                .in(CollUtil.isNotEmpty(query.getOwnerIds()), Pipeline::getOwnerId, query.getOwnerIds())
                .eq(ObjUtil.isNotEmpty(query.getIsEnabled()), Pipeline::getIsEnabled, query.getIsEnabled())
                .orderByDesc(Pipeline::getCreateTime);
    }
}
