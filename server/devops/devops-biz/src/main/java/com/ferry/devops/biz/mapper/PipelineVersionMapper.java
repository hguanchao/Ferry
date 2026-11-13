package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.PipelineVersionVO;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流水线版本数据访问
 *
 * <p>版本表没有团队字段，也不单独做数据权限；调用前必须由流水线侧确认可见性。
 * 列表查询刻意不取 content，避免历史版本多时把大字段全部读出来。
 */
@Mapper
public interface PipelineVersionMapper extends BaseMapper<PipelineVersion> {

    Pipeline selectPipelineInfo(@Param("pipelineId") Long pipelineId);

    int updatePipeline(@Param("pipelineId") Long pipelineId,
                        @Param("currentVersionId") Long currentVersionId,
                        @Param("currentVersionNo") Integer currentVersionNo,
                        @Param("stageCount") Integer stageCount);
}
