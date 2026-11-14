package com.ferry.common.security.config.props;

import com.ferry.common.security.constant.AuthConst;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Security属性配置类
 */
@Data
@ConfigurationProperties(prefix = "ferry.security")
public class SecurityProperties {

    /**
     * 认证请求头名称
     */
    private String tokenHeader = "Authorization";

    /**
     * Token头前缀
     */
    private String tokenPrefix = "Bearer";

    /**
     * Token有效期(秒)
     */
    private Long tokenTimeout = 3600L;

    /**
     * Token刷新时间(秒)
     */
    private Long refreshTimeout = 86400L;

    /**
     * 是否自动续签Token
     */
    private boolean isAutoRenew = true;

    /**
     * 令牌剩余有效期低于该值时自动续签（秒），须小于 token-timeout
     */
    private Long renewThreshold = 600L;

    /**
     * 登录连续失败达到该次数后锁定账号，调整需安全评审
     */
    private Integer maxFailCount = 5;

    /**
     * 登录失败锁定时长（秒），调整需安全评审
     */
    private Long lockDuration = 900L;

    /**
     * 令牌加密密钥（AES 只接受 16/24/32 字节，长度不符时加密直接抛异常）
     */
    private String secretKey = "FerryDevKeys2026";

    /**
     * 排除的API路径
     */
    private Set<String> excludePath = new HashSet<>();

    /**
     * 获取排除的路径列表
     *
     * <p>默认排除路径随取值并入：使用方在配置里另写 excludePath 时不会覆盖掉
     * 登录、内部接口这些必备放行项，漏配也不至于把它们挡在鉴权外。
     *
     * @return 排除的路径列表
     */
    public Set<String> getExcludePath() {
        this.excludePath.addAll(AuthConst.DEFAULT_EXCLUDE_PATH);
        return this.excludePath;
    }
}
