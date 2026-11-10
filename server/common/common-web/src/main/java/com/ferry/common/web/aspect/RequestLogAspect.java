package com.ferry.common.web.aspect;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.web.utils.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MVC请求日志AOP打印
 *
 * <p>由 WebAutoConfiguration 显式注册，只在开发与测试环境生效。
 */
@Slf4j
@Aspect
@Profile({SysConst.DEV, SysConst.TEST})
public class RequestLogAspect {
    private static final List<String> HEADERS = CollUtil.asList("Tenant-Id", HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE, HttpHeaders.USER_AGENT);

    /**
     * 切点
     */
    @Pointcut("execution(!static com.ferry.common.core.http.R *(..)) && " +
            "(@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.web.bind.annotation.RestController))")
    public void pointCut() {
    }

    /**
     * AOP环切打印请求日志
     *
     * @param point JoinPoint
     * @return Object
     * @throws Throwable 异常
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = WebUtil.getRequest();
        String requestUri = Objects.requireNonNull(request).getRequestURI();
        String requestMethod = request.getMethod();
        // 打印请求路径和请求参数
        StringBuilder requestLog = new StringBuilder(300);
        List<Object> requestArgs = new ArrayList<>();
        requestLog.append("\n================  Request Start  ================\n");
        requestLog.append("===> {} {} {}\n");
        requestArgs.add(requestMethod);
        requestArgs.add(requestUri);
        if (StrUtil.isBlank(request.getContentType())) {
            String queryString = request.getQueryString();
            requestArgs.add(StrUtil.isNotBlank(queryString) ? queryString : StrConst.EMPTY);
        } else {
            requestArgs.add(parseArgs(point));
        }
        // 客户端ip
        requestLog.append("===   IP  ===  {}\n");
        requestArgs.add(WebUtil.getIp(request));
        // 打印关键请求头
        for (String key : HEADERS) {
            String value = request.getHeader(key);
            if (StrUtil.isNotBlank(value)) {
                requestLog.append("===Headers===  {} : {}\n");
                requestArgs.add(key);
                requestArgs.add(value);
            }
        }
        requestLog.append("================  Request End   ================\n");
        // 打印执行时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info(requestLog.toString(), requestArgs.toArray());

        // 打印请求响应
        StringBuilder responseLog = new StringBuilder(200);
        List<Object> responseArgs = new ArrayList<>();
        responseLog.append("\n================  Response Start  ================\n");
        Object proceed;
        try {
            proceed = point.proceed();
            stopWatch.stop();
            String result = JsonUtil.toJson(proceed);
            responseLog.append("<=== {} {} ({} ms)\n");
            responseArgs.add(requestMethod);
            responseArgs.add(requestUri);
            responseArgs.add(stopWatch.getTotalTimeMillis());
            responseLog.append("===Result===  {}\n");
            responseArgs.add(result);
        } catch (Exception e) {
            responseLog.append("<=== {} {} ({} ms)\n");
            responseArgs.add(requestMethod);
            responseArgs.add(requestUri);
            responseArgs.add(stopWatch.getTotalTimeMillis());
            responseLog.append("===Exception===  {}\n");
            responseArgs.add(e.getMessage());
            throw e;
        } finally {
            responseLog.append("================  Response End   ================\n");
            log.info(responseLog.toString(), responseArgs.toArray());
        }
        return proceed;
    }

    /**
     * 解析请求中的参数
     *
     * @param point JoinPoint
     * @return 参数字符串
     */
    private String parseArgs(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        Method method = ((MethodSignature) point.getSignature()).getMethod();

        Map<String, Object> argMap = new HashMap<>(IntConst.SIXTEEN);
        if (Objects.nonNull(args) && args.length > IntConst.ZERO) {
            for (int i = IntConst.ZERO, len = args.length; i < len; i++) {
                Object arg = args[i];
                MethodParameter methodParameter = new MethodParameter(method, i);
                PathVariable pathVariable = methodParameter.getParameterAnnotation(PathVariable.class);

                // 请求响应等参数不处理
                if (pathVariable != null || arg instanceof HttpServletResponse || arg instanceof InputStream || isListFile(arg)) {
                    continue;
                }
                RequestBody requestBody = methodParameter.getParameterAnnotation(RequestBody.class);
                // 参数是Json
                if (requestBody != null && arg != null) {
                    if (arg instanceof List) {
                        argMap.put("param", arg.toString());
                    } else {
                        argMap.putAll(BeanUtil.toMap(arg));
                    }
                    continue;
                }

                // 参数是QueryParams、form-data、x-www-form-urlencoded
                if (arg instanceof HttpServletRequest) {
                    argMap.putAll(((HttpServletRequest) arg).getParameterMap());
                } else if (arg instanceof WebRequest) {
                    argMap.putAll(((WebRequest) arg).getParameterMap());
                } else if (arg instanceof MultipartFile) {
                    mf(argMap, arg);
                } else if (arg instanceof MultipartFile[]) {
                    mfs(argMap, arg);
                } else {
                    // 参数存在注解RequestParam
                    RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
                    String paraName;
                    if (requestParam != null && StrUtil.isNotBlank(requestParam.value())) {
                        paraName = requestParam.value();
                    } else if (requestParam != null && StrUtil.isNotBlank(requestParam.name())) {
                        paraName = requestParam.name();
                    } else {
                        paraName = methodParameter.getParameterName();
                    }
                    if (StrUtil.isBlank(paraName)) {
                        paraName = "arg" + i;
                    }
                    argMap.put(paraName, arg);
                }
            }
        }
        if (CollUtil.isEmpty(argMap)) {
            return StrConst.EMPTY;
        }
        return JsonUtil.toJson(argMap);
    }

    /**
     * 参数类型是否为List<File>
     *
     * @param arg 参数
     * @return true || false
     */
    private boolean isListFile(Object arg) {
        if (arg instanceof List) {
            List<?> argList = (List<?>) arg;
            if (CollUtil.isNotEmpty(argList)) {
                return argList.get(IntConst.ZERO) instanceof InputStreamSource;
            }
        }
        return false;
    }

    /**
     * 参数类型为MultipartFile
     *
     * @param argMap 参数Map
     * @param arg    参数
     */
    private void mf(Map<String, Object> argMap, Object arg) {
        MultipartFile file = (MultipartFile) arg;
        String name = file.getName();
        String fileName = file.getOriginalFilename();
        argMap.put(name, fileName);
    }

    /**
     * 参数类型为MultipartFile[]
     *
     * @param argMap 参数Map
     * @param arg    参数
     */
    private void mfs(Map<String, Object> argMap, Object arg) {
        MultipartFile[] files = (MultipartFile[]) arg;
        for (int f = IntConst.ZERO; f < files.length; f++) {
            String name = files[IntConst.ZERO].getName() + f;
            String fileName = files[f].getOriginalFilename();
            argMap.put(name, fileName);
        }
    }
}
