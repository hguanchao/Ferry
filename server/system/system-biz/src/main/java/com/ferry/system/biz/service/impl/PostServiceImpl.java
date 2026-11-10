package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.system.api.dto.PostDTO;
import com.ferry.system.api.dto.query.PostQuery;
import com.ferry.system.api.entity.Post;
import com.ferry.system.api.vo.PostVO;
import com.ferry.system.biz.mapper.PostMapper;
import com.ferry.system.biz.service.IPostService;
import com.ferry.system.biz.wrapper.PostWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    @Override
    public IPage<PostVO> pageList(PostQuery body) {
        PostWrapper wrapper = PostWrapper.build();
        IPage<Post> pages = page(Page.create(body), wrapper.lambda(body));
        return wrapper.pageVo(pages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PostDTO body) {
        Post post = BeanUtil.copy(body, Post.class);
        long count = count(Wrappers.<Post>lambdaQuery()
                .eq(Post::getCode, post.getCode())
                .ne(ObjUtil.isNotEmpty(post.getId()), Post::getId, post.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("岗位编码[" + post.getCode() + "]已存在");
        }
        return saveOrUpdate(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的岗位");
        }
        // 用户表按岗位id关联，删岗位后这些 id 会悬空，先拦住
        long userCount = baseMapper.selectUserCountByIds(ids);
        if (userCount > IntConst.ZERO) {
            throw new ServiceException("岗位下存在用户，请先调整用户所在岗位");
        }
        return removeByIds(ids);
    }
}
