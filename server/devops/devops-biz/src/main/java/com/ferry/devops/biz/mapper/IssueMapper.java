package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Issue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IssueMapper extends BaseMapper<Issue> {

    /**
     * 按(应用,指纹)查问题。
     */
    @Select("SELECT * FROM dev_issue WHERE app_id = #{appId} AND fingerprint = #{fingerprint} LIMIT 1")
    Issue byFingerprint(@Param("appId") Long appId, @Param("fingerprint") String fingerprint);

    /**
     * 统计应用各状态问题数。
     */
    @Select("SELECT status, COUNT(1) AS cnt FROM dev_issue WHERE app_id = #{appId} GROUP BY status")
    java.util.List<java.util.Map<String, Object>> statusStats(@Param("appId") Long appId);
}
