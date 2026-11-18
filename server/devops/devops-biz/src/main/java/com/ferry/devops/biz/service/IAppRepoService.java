package com.ferry.devops.biz.service;

import com.ferry.devops.api.dto.AppRepoDTO;
import com.ferry.devops.api.entity.AppRepo;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.vo.AppRepoVO;

import java.util.List;

/**
 * 应用代码源服务
 */
public interface IAppRepoService extends IService<AppRepo> {

    List<AppRepoVO> repoList(Long appId);

    boolean saveRepo(AppRepoDTO body);

    boolean deletedRepo(List<Long> ids);

    /**
     * 接入向导第二步：保存仓库并标记应用已完善
     *
     * @param appId 应用id
     * @param data  步骤数据 JSON
     */
    void saveWizardRepo(Long appId, String data);

    /** 应用是否已配置仓库 */
    boolean hasRepo(Long appId);
}
