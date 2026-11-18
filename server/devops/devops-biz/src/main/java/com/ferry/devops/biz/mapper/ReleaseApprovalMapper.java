package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ReleaseApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseApprovalMapper extends BaseMapper<ReleaseApproval> {

    @org.apache.ibatis.annotations.Select("SELECT id, node, approver, opinion, result, create_time AS createTime FROM dev_release_approval WHERE rc_id = #{rcId} ORDER BY node")
    java.util.List<java.util.Map<String, Object>> selectByRc(@Param("rcId") Long rcId);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_release_approval WHERE rc_id = #{rcId} AND result = 1")
    long countPassed(@Param("rcId") Long rcId);
}
