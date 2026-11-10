package com.ferry.common.jdbc.datapermission;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.config.props.MybatisProperties;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限条件构造
 *
 * <p>是否改写只看 Mapper 接口上的 {@link DataScope}：没标注解、表名不在注解范围内、
 * 没有登录用户（定时任务、异步线程）、超管、可看全部，这几种情况都原样放行。
 *
 * <p>可见范围取自登录时算好的会话快照，这里不查库。
 */
public class DataPermissionHandler implements MultiDataPermissionHandler {
    private final MybatisProperties properties;

    /**
     * Mapper 接口名 → 注解，避免每次 SQL 都反射一次
     */
    private final Map<String, Optional<DataScope>> annotations = new ConcurrentHashMap<>();

    public DataPermissionHandler(MybatisProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScope dataScope = resolve(mappedStatementId);
        if (dataScope == null || !matches(dataScope, table)) {
            return null;
        }
        AuthUser authUser = AuthUtil.getUser();
        if (authUser == null || IntConst.ONE.equals(authUser.getIsSuper()) || Boolean.TRUE.equals(authUser.getDataAll())) {
            return null;
        }
        // 注解上没写列名时用全局默认配置
        MybatisProperties.DataPermission defaultConfig = properties.getDataPermission();
        String deptColumn = StrUtil.defaultIfBlank(dataScope.deptColumn(), defaultConfig.getDeptColumn());
        String userColumn = StrUtil.defaultIfBlank(dataScope.userColumn(), defaultConfig.getUserColumn());
        Expression deptExpression = buildDeptExpression(table, deptColumn, authUser.getDataDeptIds());
        Expression userExpression = buildUserExpression(table, userColumn, authUser.getDataSelf(), authUser.getId());
        if (deptExpression == null && userExpression == null) {
            // 既看不到任何部门、也不看本人创建，必须返回空集；放行等于把数据权限关掉
            return new EqualsTo(new LongValue(IntConst.ONE), new LongValue(IntConst.ZERO));
        }
        if (deptExpression == null) {
            return userExpression;
        }
        if (userExpression == null) {
            return deptExpression;
        }
        return new ParenthesedExpressionList<>(new OrExpression(deptExpression, userExpression));
    }

    /**
     * 取 Mapper 接口上的注解
     *
     * @param mappedStatementId 形如 com.ferry.xxx.Mapper.method
     * @return 注解，未标注时返回 null
     */
    private DataScope resolve(String mappedStatementId) {
        int index = mappedStatementId.lastIndexOf('.');
        if (index <= 0) {
            return null;
        }
        return annotations.computeIfAbsent(mappedStatementId.substring(0, index), name -> {
            try {
                return Optional.ofNullable(Class.forName(name).getAnnotation(DataScope.class));
            } catch (ClassNotFoundException e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * 判断表是否在注解声明的范围内
     *
     * @param dataScope 注解
     * @param table     表
     * @return 是否命中
     */
    private boolean matches(DataScope dataScope, Table table) {
        String name = table.getName();
        for (String item : dataScope.value()) {
            if (item.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private Expression buildDeptExpression(Table table, String column, Set<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return null;
        }
        List<LongValue> values = deptIds.stream().map(LongValue::new).toList();
        return new InExpression(new Column(table, column), new ParenthesedExpressionList<>(values));
    }

    private Expression buildUserExpression(Table table, String column, Boolean self, Long userId) {
        if (!Boolean.TRUE.equals(self) || userId == null) {
            return null;
        }
        return new EqualsTo(new Column(table, column), new LongValue(userId));
    }
}
