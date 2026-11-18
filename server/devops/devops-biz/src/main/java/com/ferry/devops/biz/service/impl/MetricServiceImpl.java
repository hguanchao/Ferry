package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.MetricQueryDTO;
import com.ferry.devops.api.dto.query.MetricQuery;
import com.ferry.devops.api.entity.MetricDaily;
import com.ferry.devops.api.entity.StageStat;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.DoraVO;
import com.ferry.devops.api.vo.MetricVO;
import com.ferry.devops.api.vo.StageStatVO;
import com.ferry.devops.biz.mapper.MetricDailyMapper;
import com.ferry.devops.biz.mapper.StageStatMapper;
import com.ferry.devops.biz.service.IAiService;
import com.ferry.devops.biz.service.IMetricService;
import com.ferry.devops.biz.wrapper.MetricWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 度量服务实现
 */
@Service
@RequiredArgsConstructor
public class MetricServiceImpl extends ServiceImpl<MetricDailyMapper, MetricDaily> implements IMetricService {
    private final StageStatMapper stageStatMapper;
    private final IAiService aiService;

    @Override
    public DoraVO dora(MetricQueryDTO body) {
        return buildDora(queryMetrics(body));
    }

    @Override
    public IPage<MetricVO> pipelineEfficiency(MetricQuery body) {
        MetricWrapper wrapper = MetricWrapper.build();
        return wrapper.pageVo(page(Page.create(body), wrapper.lambda(body)));
    }

    @Override
    public IPage<StageStatVO> stageDuration(MetricQuery body) {
        IPage<StageStat> pages = stageStatMapper.selectPage(Page.create(body), Wrappers.<StageStat>lambdaQuery()
                .in(CollUtil.isNotEmpty(body.getAppIds()), StageStat::getAppId, body.getAppIds())
                .ge(body.getStart() != null, StageStat::getStatDate, body.getStart())
                .le(body.getEnd() != null, StageStat::getStatDate, body.getEnd())
                .orderByDesc(StageStat::getStatDate));
        IPage<StageStatVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        List<StageStatVO> records = new ArrayList<>();
        for (StageStat item : pages.getRecords()) {
            records.add(BeanUtil.copy(item, StageStatVO.class));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public DoraVO gateHealth(MetricQueryDTO body) {
        DoraVO vo = buildDora(queryMetrics(body));
        // 门禁健康复用豁免率与数据获取失败率，这里放入 changeFailRate 字段供前端展示
        return vo;
    }

    @Override
    public AiQualityVO aiQuality(MetricQueryDTO body) {
        return aiService.quality();
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("greeting", "你好，欢迎使用渡口交付平台");

        // 统计卡
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(stat("metric", "指标记录数", count(Wrappers.<MetricDaily>lambdaQuery()), "条", "primary"));
        result.put("stats", stats);

        // 最近活动(最小实现：空列表)
        result.put("activities", new ArrayList<>());

        // 快捷入口
        List<Map<String, Object>> shortcuts = new ArrayList<>();
        shortcuts.add(shortcut("metric", "交付度量", "查看 DORA 与流水线效率", "/devops/delivery-metric"));
        shortcuts.add(shortcut("ai", "AI 质量", "查看归因采纳率与动作统计", "/devops/ai-quality"));
        result.put("shortcuts", shortcuts);

        // 系统信息(固定结构，与前端 WorkbenchOverview.system 对齐)
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("version", "1.0.0");
        system.put("env", "dev");
        system.put("accounts", 0);
        system.put("roles", 0);
        system.put("menus", 0);
        system.put("logRetention", "365 天");
        system.put("lastBackup", "—");
        result.put("system", system);
        return result;
    }

    private Map<String, Object> stat(String key, String label, long value, String unit, String tone) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("value", value);
        item.put("unit", unit);
        item.put("tone", tone);
        return item;
    }

    private Map<String, Object> shortcut(String key, String label, String desc, String path) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("desc", desc);
        item.put("path", path);
        return item;
    }

    private List<MetricDaily> queryMetrics(MetricQueryDTO body) {
        if (body == null || CollUtil.isEmpty(body.getAppIds())) {
            return list(Wrappers.<MetricDaily>lambdaQuery().orderByDesc(MetricDaily::getStatDate));
        }
        return list(Wrappers.<MetricDaily>lambdaQuery()
                .in(MetricDaily::getAppId, body.getAppIds())
                .ge(body.getStart() != null, MetricDaily::getStatDate, body.getStart())
                .le(body.getEnd() != null, MetricDaily::getStatDate, body.getEnd())
                .orderByDesc(MetricDaily::getStatDate));
    }

    /** DORA 四项口径固定；样本不足(dataStatus=2)时以 0 值 + 数据不足标识返回 */
    private DoraVO buildDora(List<MetricDaily> metrics) {
        DoraVO vo = new DoraVO();
        if (CollUtil.isEmpty(metrics)) {
            vo.setDataStatus(2);
            vo.setDataThrough("—");
            return vo;
        }
        MetricDaily latest = metrics.get(0);
        boolean insufficient = metrics.stream().anyMatch(m -> Integer.valueOf(2).equals(m.getDataStatus()));
        vo.setDataStatus(insufficient ? 2 : 1);
        vo.setDataThrough(String.valueOf(latest.getStatDate()));
        if (insufficient) {
            return vo;
        }
        long n = metrics.size();
        long deployFreq = 0;
        long leadSum = 0;
        long mttrSum = 0;
        double failSum = 0;
        for (MetricDaily m : metrics) {
            deployFreq += m.getDeployFreq() == null ? 0 : m.getDeployFreq();
            leadSum += m.getLeadTimeP50() == null ? 0 : m.getLeadTimeP50();
            mttrSum += m.getMttrP50() == null ? 0 : m.getMttrP50();
            failSum += m.getChangeFailRate() == null ? 0 : m.getChangeFailRate().doubleValue();
        }
        vo.setDeploymentFrequency(java.math.BigDecimal.valueOf(deployFreq)
                .divide(java.math.BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP));
        vo.setLeadTimeP50(java.math.BigDecimal.valueOf(leadSum)
                .divide(java.math.BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP));
        vo.setMttrP50(java.math.BigDecimal.valueOf(mttrSum)
                .divide(java.math.BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP));
        vo.setChangeFailRate(java.math.BigDecimal.valueOf(failSum)
                .divide(java.math.BigDecimal.valueOf(n), 4, java.math.RoundingMode.HALF_UP));
        return vo;
    }
}
