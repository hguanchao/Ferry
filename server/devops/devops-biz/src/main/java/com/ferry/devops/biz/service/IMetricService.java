package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.MetricQueryDTO;
import com.ferry.devops.api.dto.query.MetricQuery;
import com.ferry.devops.api.entity.MetricDaily;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.DoraVO;
import com.ferry.devops.api.vo.MetricVO;
import com.ferry.devops.api.vo.StageStatVO;

import java.util.Map;

/**
 * 度量服务
 */
public interface IMetricService extends IService<MetricDaily> {

    /** DORA 四项指标 */
    DoraVO dora(MetricQueryDTO body);

    /** 流水线效率(复用每日指标分页展示) */
    IPage<MetricVO> pipelineEfficiency(MetricQuery body);

    /** 阶段耗时统计 */
    IPage<StageStatVO> stageDuration(MetricQuery body);

    /** 门禁健康(复用每日指标聚合) */
    DoraVO gateHealth(MetricQueryDTO body);

    /** AI 效能(复用质量看板) */
    AiQualityVO aiQuality(MetricQueryDTO body);

    /** 工作台聚合概览 */
    Map<String, Object> overview();
}
