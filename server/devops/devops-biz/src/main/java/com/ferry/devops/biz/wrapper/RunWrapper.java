package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.biz.mapper.PipelineMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 运行列表查询与VO转换
 */
public class RunWrapper extends BaseEntityWrapper<Run, RunVO> {

    private final PipelineMapper pipelineMapper;

    private RunWrapper(PipelineMapper pipelineMapper) {
        this.pipelineMapper = pipelineMapper;
    }

    public static RunWrapper build(PipelineMapper pipelineMapper) {
        return new RunWrapper(pipelineMapper);
    }

    @Override
    public RunVO vo(Run entity) {
        RunVO vo = BeanUtil.copy(entity, RunVO.class);
        if (vo != null) {
            vo.setPipelineVersionNo(entity.getPipelineVersion());
        }
        return vo;
    }

    /** 组装运行 VO：回填流水线名称、应用名称、版本号 */
    public List<RunVO> voWithExtras(List<Run> list) {
        List<RunVO> result = list.stream().map(this::vo).collect(Collectors.toList());
        Map<Long, Pipeline> pipelineMap = pipelineMap(list);
        for (RunVO vo : result) {
            Pipeline pipeline = pipelineMap.get(vo.getPipelineId());
            if (pipeline != null) {
                vo.setPipelineName(pipeline.getName());
                vo.setAppName(pipelineMapper.selectAppName(pipeline.getAppId()));
            }
        }
        return result;
    }

    private Map<Long, Pipeline> pipelineMap(List<Run> list) {
        List<Long> ids = list.stream().map(Run::getPipelineId).filter(ObjUtil::isNotEmpty).collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return pipelineMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Pipeline::getId, Function.identity(), (a, b) -> a));
    }

    /** 运行列表查询 */
    public LambdaQueryWrapper<Run> lambda(RunQuery query) {
        return Wrappers.<Run>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getAppId()), Run::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getPipelineId()), Run::getPipelineId, query.getPipelineId())
                .in(CollUtil.isNotEmpty(query.getStatuses()), Run::getStatus, query.getStatuses())
                .like(StrUtil.isNotBlank(query.getBranch()), Run::getBranch, query.getBranch())
                .orderByDesc(Run::getCreateTime);
    }
}
