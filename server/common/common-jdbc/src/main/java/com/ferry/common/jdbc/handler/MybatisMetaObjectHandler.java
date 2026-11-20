package com.ferry.common.jdbc.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.DateUtil;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Date;

/**
 * MyBatis自动填充功能。
 *
 * <p>定时任务、异步线程没有 RequestAttributes，此时跳过用户字段填充，
 * 仅填充时间与删除标记，避免 {@link AuthUtil#getUserId()} 抛出“RequestAttributes is null”。
 */
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (hasRequestContext()) {
            AuthUser authUser = AuthUtil.getUser();
            if (authUser != null) {
                this.strictInsertFill(metaObject, "tenantId", String.class, authUser.getTenantId());
                this.strictInsertFill(metaObject, "createUser", Long.class, authUser.getId());
                this.strictInsertFill(metaObject, "updateUser", Long.class, authUser.getId());
            }
        }
        this.strictInsertFill(metaObject, "createTime", Date.class, DateUtil.now());
        this.strictInsertFill(metaObject, "updateTime", Date.class, DateUtil.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, IntConst.ZERO);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (hasRequestContext()) {
            this.strictUpdateFill(metaObject, "updateUser", Long.class, AuthUtil.getUserId());
        }
        this.strictUpdateFill(metaObject, "updateTime", Date.class, DateUtil.now());
    }

    private boolean hasRequestContext() {
        return RequestContextHolder.getRequestAttributes() != null;
    }
}
