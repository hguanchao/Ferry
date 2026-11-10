package com.ferry.common.core.config;

import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.SpringUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 核心工具自动配置。
 *
 * <p>{@link SpringUtil} 与 {@link JsonUtil} 以静态字段持有容器能力，必须被容器实例化一次才能完成注入。
 * 这里显式导入，不依赖使用方的包扫描路径，保证换包名或按 jar 引用时依然生效。
 */
@AutoConfiguration
@Import({SpringUtil.class, JsonUtil.class})
public class CoreAutoConfiguration {
}
