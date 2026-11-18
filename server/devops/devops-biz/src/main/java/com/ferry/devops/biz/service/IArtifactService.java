package com.ferry.devops.biz.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.ArtifactDTO;
import com.ferry.devops.api.dto.ArtifactPromoteDTO;
import com.ferry.devops.api.dto.query.ArtifactQuery;
import com.ferry.devops.api.entity.Artifact;
import com.ferry.devops.api.vo.ArtifactDetailVO;
import com.ferry.devops.api.vo.ArtifactVO;
import com.ferry.devops.api.vo.PromotionPrecheckVO;
import com.ferry.devops.api.vo.TraceVO;

/**
 * 制品服务。
 */
public interface IArtifactService extends IService<Artifact> {

    /** 制品入库复核：查重 + 摘要反查 + 写表 + 自动写测试环境标记 */
    boolean intake(ArtifactDTO body);

    IPage<ArtifactVO> pageList(ArtifactQuery body);

    ArtifactDetailVO detail(Long id);

    TraceVO trace(Long id);

    List<ArtifactVO> search(String keyword);

    PromotionPrecheckVO promotePrecheck(Long id, Long targetEnvId);

    boolean promote(ArtifactPromoteDTO body);

    boolean restore(Long id);
}
