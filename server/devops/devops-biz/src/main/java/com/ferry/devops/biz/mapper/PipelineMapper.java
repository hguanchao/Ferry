package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.PipelineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PipelineMapper extends BaseMapper<Pipeline> {

    /**
     * 批量判断流水线是否存在进行中的运行（SQL 见 PipelineMapper.xml）。
     *
     * @param ids 流水线 id 列表，调用方需保证非空
     * @return 进行中的运行数量
     */
    long countActiveRun(@Param("ids") List<Long> ids);

    /**
     * 应用名称（列表回填）
     */
    @Select("SELECT name FROM dev_app WHERE is_deleted = 0 AND id = #{appId} LIMIT 1")
    String selectAppName(@Param("appId") Long appId);

    /**
     * 分页查询流水线列表（XML 联表应用名称/分支；运行状态与指标卡过滤见 XML 注释）。
     *
     * @param page  分页参数
     * @param query 筛选条件
     * @return 流水线列表项分页结果
     */
    IPage<PipelineVO> selectPageList(IPage<PipelineVO> page, @Param("query") PipelineQuery query);
}
