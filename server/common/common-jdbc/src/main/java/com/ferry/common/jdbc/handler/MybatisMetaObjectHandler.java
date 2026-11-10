package com.ferry.common.jdbc.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.DateUtil;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MyBatis自动填充功能
 */
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        AuthUser authUser = AuthUtil.getUser();
        if (authUser != null) {
            this.strictInsertFill(metaObject, "tenantId", String.class, authUser.getTenantId());
            this.strictInsertFill(metaObject, "createUser", Long.class, authUser.getId());
            this.strictInsertFill(metaObject, "updateUser", Long.class, authUser.getId());
        }
        this.strictInsertFill(metaObject, "createTime", Date.class, DateUtil.now());
        this.strictInsertFill(metaObject, "updateTime", Date.class, DateUtil.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, IntConst.ZERO);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateUser", Long.class, AuthUtil.getUserId());
        this.strictUpdateFill(metaObject, "updateTime", Date.class, DateUtil.now());
    }
}
