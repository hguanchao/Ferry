package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.DeployPrecheckDTO;
import com.ferry.devops.api.dto.DeployStartDTO;
import com.ferry.devops.api.dto.query.DeployQuery;
import com.ferry.devops.api.entity.DeployRecord;
import com.ferry.devops.api.vo.DeployBatchVO;
import com.ferry.devops.api.vo.DeployDetailVO;
import com.ferry.devops.api.vo.DeployRecordVO;
import com.ferry.devops.api.vo.DeployStartVO;
import com.ferry.devops.api.vo.HealthSampleVO;
import com.ferry.devops.api.vo.PrecheckTokenVO;

import java.util.List;

/**
 * 部署服务
 */
public interface IDeployService extends IService<DeployRecord> {

    /** 部署前五项校验，返回含令牌的结果 */
    PrecheckTokenVO precheck(DeployPrecheckDTO body);

    /** 启动部署 */
    DeployStartVO start(DeployStartDTO body);

    IPage<DeployRecordVO> pageList(DeployQuery body);

    DeployDetailVO detail(Long id);

    List<DeployBatchVO> batches(Long id);

    boolean pause(Long id);

    boolean resume(Long id);

    List<HealthSampleVO> health(Long deployId, Long batchId);

    /** 熔断后强制继续（生产需单独校验，由 Controller 层 @PreAuthorize 控制） */
    boolean circuitContinue(Long deployId, String reason);

    /** 上传一条健康采样（供灰度健康判定；简化为直接写采样结果） */
    boolean saveHealthSample(Long deployId, Long batchId, Integer checkType, String value, String threshold, Integer conclusion);
}
