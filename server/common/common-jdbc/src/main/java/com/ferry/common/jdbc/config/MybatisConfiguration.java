package com.ferry.common.jdbc.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ferry.common.jdbc.config.props.MybatisProperties;
import com.ferry.common.jdbc.datapermission.DataPermissionHandler;
import com.ferry.common.jdbc.handler.MybatisMetaObjectHandler;
import com.ferry.common.jdbc.handler.MybatisTenantLineHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * MyBatis配置。
 *
 * <p>只装配插件与填充器，不做 Mapper 扫描：扫描范围属于使用方业务，由各服务启动类自行声明。
 * 依赖数据源就绪后再注册，避免没有数据源时留下一批空转的拦截器。
 */
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties({MybatisProperties.class})
@AutoConfiguration(after = {DataSourceAutoConfiguration.class}, before = {MybatisPlusAutoConfiguration.class})
public class MybatisConfiguration {
    private final MybatisProperties properties;

    /**
     * 构造器注入配置
     *
     * @param properties MyBatis配置
     */
    public MybatisConfiguration(MybatisProperties properties) {
        this.properties = properties;
    }

    /**
     * 审计字段自动填充
     *
     * @return 填充器
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler mybatisMetaObjectHandler() {
        return new MybatisMetaObjectHandler();
    }

    /**
     * 租户拦截器
     *
     * @return 租户拦截器
     */
    @Bean
    @ConditionalOnMissingBean(TenantLineInnerInterceptor.class)
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new MybatisTenantLineHandler(properties));
    }

    /**
     * 分页拦截器
     *
     * @return 分页拦截器
     */
    @Bean
    @ConditionalOnMissingBean(PaginationInnerInterceptor.class)
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        // 方言由配置指定，避免每次执行都从连接元数据推断
        paginationInnerInterceptor.setDbType(properties.getDbType());
        paginationInnerInterceptor.setMaxLimit(properties.getMaxLimit());
        paginationInnerInterceptor.setOverflow(properties.getOverflow());
        return paginationInnerInterceptor;
    }

    /**
     * 数据权限拦截器
     *
     * @return 数据权限拦截器
     */
    @Bean
    @ConditionalOnMissingBean(DataPermissionInterceptor.class)
    public DataPermissionInterceptor dataPermissionInterceptor() {
        return new DataPermissionInterceptor(new DataPermissionHandler(properties));
    }

    /**
     * mybatis-plus 拦截器集合
     *
     * @return 拦截器集合
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户必须在分页之前，分页 count 才能带上租户条件
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor());
        interceptor.addInnerInterceptor(dataPermissionInterceptor());
        // 添加分页拦截器
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        // 添加防全表更新与删除拦截器
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
