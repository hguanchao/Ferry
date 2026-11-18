package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.TemplateQuery;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.biz.mapper.PipelineTemplateVersionMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模板列表查询与VO转换
 */
public class PipelineTemplateWrapper extends BaseEntityWrapper<PipelineTemplate, PipelineTemplateVO> {

    private final PipelineTemplateVersionMapper versionMapper;

    private PipelineTemplateWrapper(PipelineTemplateVersionMapper versionMapper) {
        this.versionMapper = versionMapper;
    }

    public static PipelineTemplateWrapper build(PipelineTemplateVersionMapper versionMapper) {
        return new PipelineTemplateWrapper(versionMapper);
    }

    @Override
    public PipelineTemplateVO vo(PipelineTemplate entity) {
        return BeanUtil.copy(entity, PipelineTemplateVO.class);
    }

    /**
     * 组装模板 VO：回填当前版本号、阶段数、变量数
     */
    public List<PipelineTemplateVO> voWithExtras(List<PipelineTemplate> list) {
        List<PipelineTemplateVO> result = listVo(list);
        Map<Long, PipelineTemplateVersion> versionMap = versionMap(list);
        for (PipelineTemplateVO vo : result) {
            PipelineTemplateVersion version = versionMap.get(vo.getId());
            if (version != null) {
                vo.setCurrentVersionNo(version.getVersionNo());
                vo.setStageCount(countStages(version.getContentJson()));
            }
        }
        return result;
    }

    private Map<Long, PipelineTemplateVersion> versionMap(List<PipelineTemplate> list) {
        List<Long> ids = list.stream().map(PipelineTemplate::getCurrentVersionId).filter(ObjUtil::isNotEmpty).collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return versionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(PipelineTemplateVersion::getId, Function.identity(), (a, b) -> a));
    }

    private Integer countStages(String contentJson) {
        if (StrUtil.isBlank(contentJson)) {
            return 0;
        }
        tools.jackson.databind.JsonNode stages = com.ferry.common.core.utils.JsonUtil.getNode(contentJson, "stages");
        return stages != null && stages.isArray() ? stages.size() : 0;
    }

    /**
     * 模板列表查询
     */
    public LambdaQueryWrapper<PipelineTemplate> lambda(TemplateQuery query) {
        return Wrappers.<PipelineTemplate>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getScope()), PipelineTemplate::getScope, query.getScope())
                .eq(ObjUtil.isNotEmpty(query.getTeamId()), PipelineTemplate::getTeamId, query.getTeamId())
                .like(StrUtil.isNotBlank(query.getName()), PipelineTemplate::getName, query.getName())
                .eq(ObjUtil.isNotEmpty(query.getIsEnabled()), PipelineTemplate::getIsEnabled, query.getIsEnabled())
                .in(CollUtil.isNotEmpty(query.getIds()), PipelineTemplate::getId, query.getIds())
                .orderByDesc(PipelineTemplate::getCreateTime);
    }
}
