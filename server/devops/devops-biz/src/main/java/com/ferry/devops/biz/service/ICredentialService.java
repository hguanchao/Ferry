package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.CredentialDTO;
import com.ferry.devops.api.dto.query.CredentialQuery;
import com.ferry.devops.api.entity.Credential;
import com.ferry.devops.api.vo.CredentialVO;

import java.util.List;

/**
 * 凭据管理服务
 */
public interface ICredentialService extends IService<Credential> {

    IPage<CredentialVO> pageList(CredentialQuery body);

    boolean save(CredentialDTO body);

    boolean rotate(Long id, String value);

    boolean deleted(List<Long> ids);
}
