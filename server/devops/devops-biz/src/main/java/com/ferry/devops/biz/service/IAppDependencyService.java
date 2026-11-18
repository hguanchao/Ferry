package com.ferry.devops.biz.service;

import com.ferry.devops.api.dto.AppDependencyDTO;
import com.ferry.devops.api.entity.AppDependency;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.vo.AppDependencyVO;

import java.util.List;

/**
 * 应用依赖服务服务
 */
public interface IAppDependencyService extends IService<AppDependency> {

    List<AppDependencyVO> dependencyList(Long appId);

    boolean saveDependency(AppDependencyDTO body);

    boolean deletedDependency(List<Long> ids);
}
