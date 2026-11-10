package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 统计关联这些岗位的用户数量
     *
     * @param ids 岗位id集合
     * @return long 用户数量
     */
    long selectUserCountByIds(@Param("ids") List<Long> ids);
}