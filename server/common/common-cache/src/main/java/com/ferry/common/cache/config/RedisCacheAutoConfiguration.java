package com.ferry.common.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.ferry.common.cache.utils.LockUtil;
import com.ferry.common.cache.utils.RedisUtil;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.utils.CollUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
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
 * <p>同时导入 {@link RedisUtil} 与 {@link LockUtil}：两者都用静态字段持有客户端，必须由容器实例化一次才能完成注入。
 *
 * <p>Redisson 只用核心包，starter 绑定的是 Boot 3，因此这里按 Boot 的 Redis 连接参数
 * 自行构建 {@link RedissonClient}，保证锁与缓存指向同一个 Redis。
 */
@EnableCaching
@Import({RedisUtil.class, LockUtil.class})
@EnableConfigurationProperties({CacheProperties.class, DataRedisProperties.class})
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
     * Redisson 客户端
     *
     * <p>连接参数复用 Boot 的 {@code spring.data.redis.*}，与缓存保持一致；
     * 只支持单机、哨兵、集群三种形态，每种形态的配置在一次调用里装配完成，
     * 不拆成「先建再补」，避免重复调用 useXxxServers() 把前一次配置覆盖掉。
     *
     * @param properties Redis 连接参数
     * @return Redisson 客户端
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(DataRedisProperties properties) {
        Config config = new Config();
        // 命令超时与连接超时取 Boot 配置，未配置时给一个不敏感于网络抖动的下限
        Duration timeout = properties.getTimeout() == null ? Duration.ofSeconds(3) : properties.getTimeout();
        Duration connectTimeout = properties.getConnectTimeout() == null ? timeout : properties.getConnectTimeout();

        DataRedisProperties.Cluster cluster = properties.getCluster();
        DataRedisProperties.Sentinel sentinel = properties.getSentinel();
        if (cluster != null && CollUtil.isNotEmpty(cluster.getNodes())) {
            config.useClusterServers()
                    .addNodeAddress(cluster.getNodes().toArray(new String[0]))
                    .setPassword(properties.getPassword())
                    .setUsername(properties.getUsername())
                    .setTimeout((int) timeout.toMillis())
                    .setConnectTimeout((int) connectTimeout.toMillis());
        } else if (sentinel != null && CollUtil.isNotEmpty(sentinel.getNodes())) {
            config.useSentinelServers()
                    .setMasterName(sentinel.getMaster())
                    .addSentinelAddress(sentinel.getNodes().toArray(new String[0]))
                    .setPassword(properties.getPassword())
                    .setUsername(properties.getUsername())
                    .setTimeout((int) timeout.toMillis())
                    .setConnectTimeout((int) connectTimeout.toMillis());
        } else {
            config.useSingleServer()
                    .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                    .setDatabase(properties.getDatabase())
                    .setPassword(properties.getPassword())
                    .setUsername(properties.getUsername())
                    .setTimeout((int) timeout.toMillis())
                    .setConnectTimeout((int) connectTimeout.toMillis());
        }
        return Redisson.create(config);
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
