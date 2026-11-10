package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 统计关联该岗位的用户数量
     *
     * @param id 岗位id
     * @return long 用户数量
     */
    long selectUserCountById(@Param("id") Long id);
}