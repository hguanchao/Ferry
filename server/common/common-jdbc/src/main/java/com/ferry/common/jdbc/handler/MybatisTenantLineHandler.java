package com.ferry.common.jdbc.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.config.props.MybatisProperties;
import com.ferry.common.security.utils.AuthUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.List;

/**
 * MyBatis租户处理器
 */
public class MybatisTenantLineHandler implements TenantLineHandler {
    private final MybatisProperties properties;

    /**
     * 默认要添加租户的表
     */
    private static final List<String> DEFAULT_CONCERN_TABLES = CollUtil.asList("sys_config", "sys_dept", "sys_dict", "sys_menu", "sys_post", "sys_role", "sys_user");

    public MybatisTenantLineHandler(MybatisProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getTenantId() {
        String tenantId = AuthUtil.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            tenantId = StrConst.EMPTY;
        }
        return new StringValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return properties.getTenant().getColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        DEFAULT_CONCERN_TABLES.addAll(properties.getTenant().getTables());
        return !DEFAULT_CONCERN_TABLES.contains(tableName);
    }
}
