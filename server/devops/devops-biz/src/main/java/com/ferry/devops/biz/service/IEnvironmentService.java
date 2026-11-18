package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.EnvironmentDTO;
import com.ferry.devops.api.dto.EnvTypeChangeApplyDTO;
import com.ferry.devops.api.dto.EnvTypeChangeApproveDTO;
import com.ferry.devops.api.dto.query.EnvironmentQuery;
import com.ferry.devops.api.entity.Environment;
import com.ferry.devops.api.vo.EnvironmentVO;
import com.ferry.devops.api.vo.EnvOptionVO;

import java.util.List;
import java.util.Map;

/**
 * 环境管理服务
 */
public interface IEnvironmentService extends IService<Environment> {

    IPage<EnvironmentVO> pageList(EnvironmentQuery body);

    EnvironmentVO detail(Long id);

    boolean save(EnvironmentDTO body);

    boolean deleted(List<Long> ids);

    List<EnvOptionVO> options(Long appId);

    boolean applyTypeChange(EnvTypeChangeApplyDTO body);

    boolean approveTypeChange(EnvTypeChangeApproveDTO body);

    /**
     * 内部接口：环境id到名称的映射，按应用过滤
     */
    Map<String, String> optionMap(Long appId);
}
