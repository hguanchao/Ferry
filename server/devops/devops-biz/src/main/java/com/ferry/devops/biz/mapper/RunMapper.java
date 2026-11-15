package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.dto.query.RunTriggerRecordQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流水线运行数据访问
 */
@Mapper
@DataScope(value = "dev_app", deptColumn = "dept_id")
public interface RunMapper extends BaseMapper<Run> {

    /**
     * 分页查询运行列表
     *
     * <p>运行挂应用，数据权限按应用所属部门过滤，与流水线列表同一口径。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 运行列表
     */
    IPage<RunVO> selectPageList(IPage<RunVO> page, @Param("query") RunQuery query);

    /**
     * 分页查询触发记录
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 触发记录列表
     */
    IPage<TriggerRecordVO> selectTriggerPageList(IPage<TriggerRecordVO> page, @Param("query") RunTriggerRecordQuery query);
}
