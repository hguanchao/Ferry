package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.OpenTokenDTO;
import com.ferry.devops.api.dto.query.OpenTokenQuery;
import com.ferry.devops.api.entity.OpenToken;
import com.ferry.devops.api.vo.OpenTokenVO;

/**
 * 开放 API Token 服务
 */
public interface IOpenApiService extends IService<OpenToken> {

    IPage<OpenTokenVO> tokenPage(OpenTokenQuery query);

    /** 签发 Token，明文仅返回一次 */
    OpenTokenVO issue(OpenTokenDTO body);

    /** 轮换：旧值立即失效，返回新明文 */
    OpenTokenVO rotate(Long id);

    /** 禁用 */
    boolean disable(Long id);
}
