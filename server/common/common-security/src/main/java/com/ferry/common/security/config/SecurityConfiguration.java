package com.ferry.common.security.config;

import com.ferry.common.security.aspect.PreAuthorizeAspect;
import com.ferry.common.security.config.props.SecurityProperties;
import com.ferry.common.security.filter.AuthFilter;
import com.ferry.common.security.utils.AuthUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Security自动配置。
 *
 * <p>鉴权过滤器改为显式注册，既不必依赖使用方的包扫描，也便于控制生效路径与替换方式。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({SecurityProperties.class})
@Import(AuthUtil.class)
public class SecurityConfiguration {

    /**
     * 鉴权过滤器注册
     *
     * @param properties 安全配置
     * @return 过滤器注册对象
     */
    @Bean
    @ConditionalOnMissingBean(name = "authFilterRegistration")
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(SecurityProperties properties) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>(new AuthFilter(properties));
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 接口权限校验切面注册
     *
     * <p>与过滤器同样显式注册，不依赖使用方的包扫描。
     *
     * @return 权限校验切面
     */
    @Bean
    @ConditionalOnMissingBean
    public PreAuthorizeAspect preAuthorizeAspect() {
        return new PreAuthorizeAspect();
    }
}
