package com.ferry.common.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.ferry.common.cache.utils.RedisUtil;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis缓存自动配置。
 *
 * <p>排在 Boot 的 Redis 与 Cache 自动配置之前：先提供带 Jackson 序列化的模板与缓存管理器，
 * Boot 侧看到同类型 Bean 后自动退让，避免两套 CacheManager 竞争导致序列化方式不确定。
 *
 * <p>同时导入 {@link RedisUtil}：它用静态字段持有模板，必须由容器实例化一次才能完成注入。
 */
@EnableCaching
@Import(RedisUtil.class)
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
@AutoConfiguration(before = {DataRedisAutoConfiguration.class, CacheAutoConfiguration.class})
public class RedisCacheAutoConfiguration {

    /**
     * redis模板配置
     *
     * @param connectionFactory 连接工厂
     * @return RedisTemplate Bean
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // key 序列化
        StringRedisSerializer redisKeySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(redisKeySerializer);
        redisTemplate.setHashKeySerializer(redisKeySerializer);
        JacksonJsonRedisSerializer<Object> redisValueSerializer = jacksonJsonRedisSerializer();
        redisTemplate.setValueSerializer(redisValueSerializer);
        redisTemplate.setHashValueSerializer(redisValueSerializer);
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    /**
     * Spring内置的Cache配置
     *
     * @param properties    属性配置类
     * @param redisTemplate redis模板
     * @return CacheManager
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(CacheProperties properties,
                                     RedisTemplate<String, Object> redisTemplate) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(IntConst.SEVEN))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisTemplate.getStringSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisTemplate.getValueSerializer()));
        CacheProperties.Redis redisProperties = properties.getRedis();
        if (redisProperties.getTimeToLive() != null) {
            configuration = configuration.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            String keyPrefix = redisProperties.getKeyPrefix();
            if (!keyPrefix.endsWith(StrConst.COLON)) {
                keyPrefix += StrConst.COLON;
            }
            configuration = configuration.prefixCacheNameWith(keyPrefix);
        }
        if (!redisProperties.isCacheNullValues()) {
            configuration = configuration.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            configuration = configuration.disableKeyPrefix();
        }
        return RedisCacheManager.builder(Objects.requireNonNull(redisTemplate.getConnectionFactory()))
                .cacheDefaults(configuration).build();
    }

    private JacksonJsonRedisSerializer<Object> jacksonJsonRedisSerializer() {
        // value 序列化：Jackson 3 的 Mapper 不可变，只能从构建器一次性生成专用实例，
        // 不能复制容器内的 Mapper —— 容器 Mapper 会把 Long 输出为字符串，写入缓存后无法还原类型。
        JsonMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(value -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
                .changeDefaultVisibility(checker -> checker.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
                // 开启默认类型信息，否则嵌套对象反序列化会被还原成 LinkedHashMap
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(), DefaultTyping.NON_FINAL)
                // 忽略 JSON 中存在而 Java 对象没有的属性，兼容对象字段下线后的存量缓存
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        return new JacksonJsonRedisSerializer<>(mapper, Object.class);
    }
}
