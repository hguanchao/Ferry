package com.ferry.common.ai.config;

import com.ferry.common.ai.config.props.AiProperties;
import com.ferry.common.ai.utils.AiUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * AI 工具类自动配置。
 *
 * <p>各服务引入 common-ai 即自动导入 {@link AiUtil}：其静态依赖在容器启动时由构造器注入，
 * 业务侧直接静态调用，无需声明 Bean 或注入字段。缺少 Spring AI 依赖时整个配置不生效。
 */
@AutoConfiguration
@EnableConfigurationProperties(AiProperties.class)
@ConditionalOnClass(ChatClient.class)
@Import(AiUtil.class)
public class AiAutoConfiguration {
}
