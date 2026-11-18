package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.ReleaseApproveDTO;
import com.ferry.devops.api.dto.ReleaseArtifactDTO;
import com.ferry.devops.api.dto.ReleaseCandidateDTO;
import com.ferry.devops.api.dto.ReleaseCheckDTO;
import com.ferry.devops.api.dto.ReleaseTerminateDTO;
import com.ferry.devops.api.dto.query.ReleaseQuery;
import com.ferry.devops.api.entity.ReleaseCandidate;
import com.ferry.devops.api.vo.ArtifactCandidateVO;
import com.ferry.devops.api.vo.ImpactVO;
import com.ferry.devops.api.vo.ReleaseCheckVO;
import com.ferry.devops.api.vo.ReleaseDetailVO;
import com.ferry.devops.api.vo.ReleaseRecordVO;
import com.ferry.devops.api.vo.ReleaseTimelineVO;
import com.ferry.devops.api.vo.ReleaseVO;
import com.ferry.devops.api.vo.RiskVO;

import java.util.List;

/**
 * 发布单与生产发布服务
 */
public interface IReleaseService extends IService<ReleaseCandidate> {

    IPage<ReleaseVO> pageList(ReleaseQuery body);

    ReleaseDetailVO detail(Long id);

    boolean save(ReleaseCandidateDTO body);

    boolean deleted(List<Long> ids);

    /** 可选制品（已晋级预发 + 门禁通过/豁免） */
    List<ArtifactCandidateVO> candidates(Long appId);

    boolean saveArtifacts(ReleaseArtifactDTO body);

    boolean submit(Long id);

    boolean withdraw(Long id, String reason);

    ImpactVO impact(Long id);

    RiskVO risk(Long id);

    List<ReleaseCheckVO> checks(Long id);

    boolean saveCheck(ReleaseCheckDTO body);

    boolean approve(ReleaseApproveDTO body);

    boolean reject(Long id, String opinion, String returnStage);

    List<ReleaseTimelineVO> timeline(Long id);

    /** 启动生产发布，返回发布记录id */
    Long start(Long id, Long confirmer);

    boolean terminate(ReleaseTerminateDTO body);

    IPage<ReleaseRecordVO> recordPage(ReleaseQuery body);
}
