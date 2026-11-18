package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Credential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CredentialMapper extends BaseMapper<Credential> {

    /**
     * 统计凭据被环境配置引用的真实数量
     */
    @Select("SELECT COUNT(1) FROM dev_env_config WHERE is_deleted = 0 "
            + "AND value_type = 2 AND credential_id = #{credentialId}")
    long countRef(@Param("credentialId") Long credentialId);
}
