package com.ferry.common.web.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Swagger Properties属性类
 */
@Data
@ConfigurationProperties("swagger")
public class SwaggerProperties {

    /**
     * 标题
     */
    private String title = "Ferry接口文档";

    /**
     * 描述
     */
    private String description = "Ferry接口文档";

    /**
     * 版本
     */
    private String version = "1.0.0";

    /**
     * 许可证
     */
    private String license = "Apache 2.0";

    /**
     * 许可证URL
     */
    private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.html";

    /**
     * 联系人信息
     */
    private Contact contact = new Contact();

    @Data
    public static class Contact {
        /**
         * 联系人
         */
        private String name = "Chao";
        /**
         * 联系人url
         */
        private String url = "https://github.com/hguanchao";
        /**
         * 联系人email
         */
        private String email = "hguanchao@yeah.net";
    }
}