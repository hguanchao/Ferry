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
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.biz.mapper.PipelineMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水线列表查询与VO转换
 */
public class PipelineWrapper extends BaseEntityWrapper<Pipeline, PipelineVO> {

    private final PipelineMapper mapper;

    private PipelineWrapper(PipelineMapper mapper) {
        this.mapper = mapper;
    }

    public static PipelineWrapper build(PipelineMapper mapper) {
        return new PipelineWrapper(mapper);
    }

    @Override
    public PipelineVO vo(Pipeline entity) {
        return BeanUtil.copy(entity, PipelineVO.class);
    }

    /** 组装列表 VO：回填应用名称 */
    public List<PipelineVO> voWithExtras(List<Pipeline> list) {
        List<PipelineVO> result = list.stream().map(this::vo).collect(Collectors.toList());
        for (PipelineVO vo : result) {
            vo.setAppName(mapper.selectAppName(vo.getAppId()));
        }
        return result;
    }

    /** 按流水线列表筛选条件构造查询 */
    public LambdaQueryWrapper<Pipeline> lambda(PipelineQuery query) {
        return Wrappers.<Pipeline>lambdaQuery()
                .in(CollUtil.isNotEmpty(query.getAppIds()), Pipeline::getAppId, query.getAppIds())
                .like(StrUtil.isNotBlank(query.getName()), Pipeline::getName, query.getName())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), Pipeline::getStatus, query.getStatus())
                .orderByDesc(Pipeline::getCreateTime);
    }
}
