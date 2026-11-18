package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.AiOutput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI 输出 Mapper
 */
@Mapper
public interface AiOutputMapper extends BaseMapper<AiOutput> {

    /** 输出总数 */
    @Select("SELECT COUNT(1) FROM dev_ai_output WHERE is_deleted = 0")
    long countAll();

    /** 采纳数：存在有帮助的反馈 */
    @Select("SELECT COUNT(DISTINCT o.id) FROM dev_ai_output o JOIN dev_ai_feedback f ON f.output_id = o.id "
            + "WHERE o.is_deleted = 0 AND f.is_deleted = 0 AND f.is_helpful = 1")
    long countAdopted();

    /** 低置信结论数 */
    @Select("SELECT COUNT(1) FROM dev_ai_output WHERE is_deleted = 0 AND confidence = 3")
    long countLowConfidence();

    /** 拒答数：无法归因 */
    @Select("SELECT COUNT(1) FROM dev_ai_output WHERE is_deleted = 0 AND status = 4")
    long countRefusal();

    /** 按业务id取最近一条成功输出 */
    @Select("SELECT * FROM dev_ai_output WHERE is_deleted = 0 AND type = #{type} AND biz_id = #{bizId} "
            + "ORDER BY create_time DESC LIMIT 1")
    AiOutput latestByBiz(@Param("type") Integer type, @Param("bizId") String bizId);
}
