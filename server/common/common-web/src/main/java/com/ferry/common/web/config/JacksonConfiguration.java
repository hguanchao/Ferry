package com.ferry.common.web.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/**
 * Jackson 配置类。
 *
 * <p>Jackson 3 的 Mapper 构建后不可变，配置只能通过构建器完成：
 * 直接在 Boot 提供的 {@code JsonMapper.Builder} 上追加模块与开关，不另建实例，
 * 避免两套 Mapper 配置漂移。</p>
 */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class JacksonConfiguration {

    /**
     * 序列化模块：超出前端 Number 精度的大数字统一按字符串输出。
     *
     * @return 序列化模块
     */
    @Bean
    public SimpleModule simpleModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        return module;
    }

    /**
     * Jackson 定制。
     *
     * <p>模块按名称注入：容器中同时存在 springdoc、MyBatis-Plus 等注册的多个
     * {@code SimpleModule}，按类型注入无法确定唯一候选。</p>
     *
     * @param module 大数字序列化模块
     * @return 定制器
     */
    @Bean
    @ConditionalOnMissingBean(JsonMapperBuilderCustomizer.class)
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(@Qualifier("simpleModule") SimpleModule module) {
        return builder -> builder
                .addModule(module)
                // 宽容解析：单引号、未转义控制字符、任意反斜杠转义
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES,
                        JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS,
                        JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
