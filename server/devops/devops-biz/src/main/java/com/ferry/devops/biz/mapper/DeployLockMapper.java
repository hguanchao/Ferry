package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.DeployLock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeployLockMapper extends BaseMapper<DeployLock> {

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_deploy_lock WHERE is_active = 1 AND app_id = #{appId} AND env_id = #{envId} AND expire_at > NOW()")
    long countActive(@Param("appId") Long appId, @Param("envId") Long envId);
}
