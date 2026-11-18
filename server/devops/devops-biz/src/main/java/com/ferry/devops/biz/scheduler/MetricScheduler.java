package com.ferry.devops.biz.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.devops.api.entity.MetricDaily;
import com.ferry.devops.biz.mapper.MetricDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 度量跑批任务。
 *
 * <p>每日 03:00 执行，按应用逐日聚合 dev_run / dev_deploy_record 写入 dev_metric_daily。
 * 最小实现：仅取有部署记录的应用，统计前一日成功部署数写入 deploy_freq；
 * 样本不足(成功数为 0)置 data_status=2。不引入 ShedLock 依赖，
 * 如需多实例选主，可在 {@link #runDailyMetric()} 上追加 ShedLock 注解。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricScheduler {
    private final MetricDailyMapper metricDailyMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void runDailyMetric() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Date start = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date statDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Long> appIds = metricDailyMapper.listActiveAppIds();
        for (Long appId : appIds) {
            long success = metricDailyMapper.countSuccessDeploy(appId, start, end);
            MetricDaily existing = metricDailyMapper.selectOne(Wrappers.<MetricDaily>lambdaQuery()
                    .eq(MetricDaily::getStatDate, statDate)
                    .eq(MetricDaily::getAppId, appId));
            MetricDaily daily = existing == null ? new MetricDaily() : existing;
            daily.setStatDate(statDate);
            daily.setAppId(appId);
            daily.setDeployFreq((int) success);
            daily.setDataStatus(success > IntConst.ZERO ? IntConst.ONE : 2);
            if (existing == null) {
                metricDailyMapper.insert(daily);
            } else {
                metricDailyMapper.updateById(daily);
            }
        }
        log.info("度量跑批完成，统计日 {}，处理应用数 {}", yesterday, appIds.size());
    }
}
