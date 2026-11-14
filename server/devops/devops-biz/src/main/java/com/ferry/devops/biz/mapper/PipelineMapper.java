package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.PipelineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流水线定义数据访问
 */
@Mapper
@DataScope(value = "dev_app", deptColumn = "dept_id")
public interface PipelineMapper extends BaseMapper<Pipeline> {

    /**
     * 分页查询流水线列表
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 流水线列表
     */
    IPage<PipelineVO> selectPageList(IPage<PipelineVO> page, @Param("query") PipelineQuery query);
}
