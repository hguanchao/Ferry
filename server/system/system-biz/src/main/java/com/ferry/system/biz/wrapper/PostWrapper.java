package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.PostQuery;
import com.ferry.system.api.entity.Post;
import com.ferry.system.api.vo.PostVO;

public class PostWrapper extends BaseEntityWrapper<Post, PostVO> {
    public static PostWrapper build() {
        return new PostWrapper();
    }

    @Override
    public PostVO vo(Post entity) {
        return BeanUtil.copy(entity, PostVO.class);
    }

    public LambdaQueryWrapper<Post> lambda(PostQuery query) {
        String tenantId = query.getTenantId();
        String code = query.getCode();
        String name = query.getName();
        Integer status = query.getStatus();
        return Wrappers.<Post>lambdaQuery()
                .eq(StrUtil.isNotBlank(tenantId), Post::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(code), Post::getCode, code)
                .like(StrUtil.isNotBlank(name), Post::getName, name)
                .eq(ObjUtil.isNotEmpty(status), Post::getStatus, status)
                .orderByAsc(Post::getSort).orderByDesc(Post::getCreateTime);
    }
}
