package com.ferry.common.web.config;

import com.ferry.common.web.aspect.RequestLogAspect;
import com.ferry.common.web.handler.ExceptionAdviceHandler;
import com.ferry.common.web.utils.WebUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * Web 层自动配置。
 *
 * <p>全局异常、请求日志与 Web 工具都需要容器实例化，集中在此导入，
 * 使公共模块脱离使用方的包扫描依然可用。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ExceptionAdviceHandler.class, WebUtil.class, RequestLogAspect.class})
public class WebAutoConfiguration {
}
