package com.ferry.common.cache.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 幂等配置。
 */
@Data
@ConfigurationProperties(prefix = "ferry.idempotent")
public class IdempotentProperties {

    /** 是否启用幂等注解 */
    private boolean enabled = true;

    /** 默认幂等窗口秒数，注解 ttl<=0 时使用 */
    private int defaultTtlSeconds = 60;
}
