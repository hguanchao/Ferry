package com.ferry.devops.biz.mapper;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.vo.RunVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RunMapper extends BaseMapper<Run> {

    /**
     * 运行分页列表：SQL 内联表回填流水线名/应用名，条件与分页见 RunMapper.xml。
     */
    IPage<RunVO> selectPageList(IPage<RunVO> page, @Param("query") RunQuery query);

    /**
     * 同一应用/引用/流水线版本是否已有活动运行（排队/运行/阻塞）
     *
     * @param appId           应用id
     * @param refValue        生效引用值
     * @param pipelineVersion 流水线版本号
     * @return 最近的活动运行，无则 null
     */
    @Select("SELECT * FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} "
            + "AND ref_value = #{refValue} AND pipeline_version = #{pipelineVersion} "
            + "AND status IN (0,1,2,3) ORDER BY id DESC LIMIT 1")
    Run selectActiveBySource(@Param("appId") Long appId, @Param("refValue") String refValue,
                             @Param("pipelineVersion") Integer pipelineVersion);

    /**
     * 同一应用/引用/流水线版本是否已有成功运行
     *
     * @param appId           应用id
     * @param refValue        生效引用值
     * @param pipelineVersion 流水线版本号
     * @return 最近的成功运行，无则 null
     */
    @Select("SELECT * FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} "
            + "AND ref_value = #{refValue} AND pipeline_version = #{pipelineVersion} "
            + "AND status = 4 ORDER BY id DESC LIMIT 1")
    Run selectSuccessBySource(@Param("appId") Long appId, @Param("refValue") String refValue,
                              @Param("pipelineVersion") Integer pipelineVersion);

    /**
     * 同一流水线是否存在进行中的运行（待确认/排队/运行/阻塞）
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND pipeline_id = #{pipelineId} AND status IN (0,1,2,3)")
    long countActiveByPipeline(@Param("pipelineId") Long pipelineId);

    /**
     * 应用最近 N 次触发次数（频率管控）
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} "
            + "AND trigger_user = #{userId} AND create_time > #{since}")
    long countByUserSince(@Param("appId") Long appId, @Param("userId") Long userId, @Param("since") Date since);

    /**
     * 生成运行编号：RUN-yyyyMMdd-当日序号（四位）
     */
    @Select("SELECT COUNT(1) + 1 FROM dev_run WHERE is_deleted = 0 AND code LIKE CONCAT('RUN-', #{day}, '-%')")
    long nextSeqOfDay(@Param("day") String day);

    /**
     * 应用历史运行出现过的分支
     */
    @Select("SELECT DISTINCT branch FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} ORDER BY branch")
    List<String> selectBranches(@Param("appId") Long appId);
}
