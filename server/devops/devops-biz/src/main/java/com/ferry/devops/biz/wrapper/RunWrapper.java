package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.PipelineVersionMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 运行列表查询与VO转换
 */
public class RunWrapper extends BaseEntityWrapper<Run, RunVO> {

    private final PipelineMapper pipelineMapper;
    private final PipelineVersionMapper versionMapper;

    private RunWrapper(PipelineMapper pipelineMapper, PipelineVersionMapper versionMapper) {
        this.pipelineMapper = pipelineMapper;
        this.versionMapper = versionMapper;
    }

    public static RunWrapper build(PipelineMapper pipelineMapper, PipelineVersionMapper versionMapper) {
        return new RunWrapper(pipelineMapper, versionMapper);
    }

    @Override
    public RunVO vo(Run entity) {
        return BeanUtil.copy(entity, RunVO.class);
    }

    /**
     * 组装运行 VO：回填流水线名称、应用名称、版本号
     */
    public List<RunVO> voWithExtras(List<Run> list) {
        List<RunVO> result = listVo(list);
        Map<Long, com.ferry.devops.api.entity.Pipeline> pipelineMap = pipelineMap(list);
        Map<Long, com.ferry.devops.api.entity.PipelineVersion> versionMap = versionMap(list);
        for (RunVO vo : result) {
            com.ferry.devops.api.entity.Pipeline pipeline = pipelineMap.get(vo.getPipelineId());
            if (pipeline != null) {
                vo.setPipelineName(pipeline.getName());
                vo.setAppName(pipelineMapper.selectAppName(pipeline.getAppId()));
            }
            com.ferry.devops.api.entity.PipelineVersion version = versionMap.get(vo.getPipelineVersionId());
            if (version != null) {
                vo.setPipelineVersionNo(version.getVersionNo());
            }
        }
        return result;
    }

    private Map<Long, com.ferry.devops.api.entity.Pipeline> pipelineMap(List<Run> list) {
        List<Long> ids = list.stream().map(Run::getPipelineId).filter(ObjUtil::isNotEmpty).collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            return java.util.Collections.emptyMap();
        }
        return pipelineMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(com.ferry.devops.api.entity.Pipeline::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, com.ferry.devops.api.entity.PipelineVersion> versionMap(List<Run> list) {
        List<Long> ids = list.stream().map(Run::getPipelineVersionId).filter(ObjUtil::isNotEmpty).collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            return java.util.Collections.emptyMap();
        }
        return versionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(com.ferry.devops.api.entity.PipelineVersion::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 运行列表查询
     */
    public LambdaQueryWrapper<Run> lambda(RunQuery query) {
        return Wrappers.<Run>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getAppId()), Run::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getPipelineId()), Run::getPipelineId, query.getPipelineId())
                .in(CollUtil.isNotEmpty(query.getStatuses()), Run::getStatus, query.getStatuses())
                .like(StrUtil.isNotBlank(query.getBranch()), Run::getBranch, query.getBranch())
                .orderByDesc(Run::getCreateTime);
    }
}
