package com.ferry.common.web.config;

import com.ferry.common.web.config.props.SwaggerProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 接口文档配置类。
 *
 * <p>文档开关跟随 springdoc 官方配置项，运行期可通过 springdoc.api-docs.enabled=false 一并关掉文档模型。
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnBooleanProperty(prefix = "springdoc.api-docs", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerConfiguration {

    /**
     * 接口文档基础信息
     *
     * @param properties 属性类
     * @return OpenAPI
     */
    @Bean
    public OpenAPI openApi(SwaggerProperties properties) {
        SwaggerProperties.Contact contact = properties.getContact();
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion())
                        .contact(new Contact().name(contact.getName()).email(contact.getEmail()).url(contact.getUrl()))
                        .license(new License().name(properties.getLicense()).url(properties.getLicenseUrl())));
    }
}
