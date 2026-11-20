package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunIdempotency;
import org.apache.ibatis.annotations.Insert;
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

    /**
     * 写入或刷新幂等键：同一键只保留一行，重复触发时覆盖为最新运行。
     *
     * <p>幂等键上有唯一索引，失败/取消/待确认等非活动运行结束后，再次触发同一提交
     * 应创建新运行并刷新该行，不能重复 INSERT 触发唯一键冲突。
     */
    @Insert("""
            INSERT INTO dev_run_idempotency (id, idempotent_key, app_id, run_id, state, expire_time)
            VALUES (#{id}, #{idempotentKey}, #{appId}, #{runId}, #{state}, #{expireTime})
            ON DUPLICATE KEY UPDATE
                app_id = VALUES(app_id),
                run_id = VALUES(run_id),
                state = VALUES(state),
                expire_time = VALUES(expire_time)
            """)
    int upsert(RunIdempotency idempotency);
}
