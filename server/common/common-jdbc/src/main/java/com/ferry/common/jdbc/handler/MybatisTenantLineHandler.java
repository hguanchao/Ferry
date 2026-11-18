package com.ferry.common.jdbc.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.config.props.MybatisProperties;
import com.ferry.common.security.utils.AuthUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MyBatis租户处理器
 */
public class MybatisTenantLineHandler implements TenantLineHandler {
    private final MybatisProperties properties;
    private final Set<String> concernTables;

    /**
     * 默认要添加租户的表：仅系统管理域表；devops 业务表不参与租户隔离
     */
    private static final List<String> DEFAULT_CONCERN_TABLES = CollUtil.asList(
            "sys_config", "sys_dept", "sys_dict", "sys_menu", "sys_post", "sys_role", "sys_user", "sys_storage_policy"
    );

    public MybatisTenantLineHandler(MybatisProperties properties) {
        this.properties = properties;
        this.concernTables = new HashSet<>(DEFAULT_CONCERN_TABLES);
        this.concernTables.addAll(properties.getTenant().getTables());
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
        return !concernTables.contains(tableName);
    }
}
