package com.ferry.devops.biz.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.ArtifactCleanPolicyDTO;
import com.ferry.devops.api.entity.ArtifactCleanPolicy;
import com.ferry.devops.api.vo.CleanPolicyVO;
import com.ferry.devops.api.vo.CleanTaskVO;

/**
 * 制品清理服务。
 */
public interface IArtifactCleanService extends IService<ArtifactCleanPolicy> {

    CleanPolicyVO policy(Long appId);

    boolean savePolicy(ArtifactCleanPolicyDTO body);

    /** 生成清理清单与任务；confirm=true 时按清单执行软删除 */
    boolean run(Long appId, Boolean confirm);

    IPage<CleanTaskVO> taskPage(Long appId, Integer current, Integer size);

    /** 清单下载内容（JSON 文本） */
    String manifest(Long taskId);
}
