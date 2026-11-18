package com.ferry.devops.biz.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务调度，供 {@link MetricScheduler} 使用。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
