package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunIdempotency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RunIdempotencyMapper extends BaseMapper<RunIdempotency> {

    /**
     * 按幂等键查记录
     */
    @Select("SELECT * FROM dev_run_idempotency WHERE idempotent_key = #{key} LIMIT 1")
    RunIdempotency selectByKey(@Param("key") String key);
}
