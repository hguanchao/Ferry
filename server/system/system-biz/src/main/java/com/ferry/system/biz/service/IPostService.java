package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.PostDTO;
import com.ferry.system.api.dto.query.PostQuery;
import com.ferry.system.api.entity.Post;
import com.ferry.system.api.vo.PostVO;

import java.util.List;

public interface IPostService extends IService<Post> {

    /**
     * 岗位管理-分页列表
     *
     * @param body 查询条件
     * @return 分页列表
     */
    IPage<PostVO> pageList(PostQuery body);

    /**
     * 岗位管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(PostDTO body);

    /**
     * 岗位管理-删除信息
     *
     * @param ids 岗位id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);
}
