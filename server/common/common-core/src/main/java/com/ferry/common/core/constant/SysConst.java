package com.ferry.common.core.constant;

/**
 * 系统常量
 */
public interface SysConst {

    String BASE_PACKAGES        = "com.ferry";
    String VERSION              = "1.0.0";
    String DEV                  = "dev";
    String PROD                 = "prod";
    String TEST                 = "test";

    String APPLICATION_GATEWAY_NAME = "ferry-gateway";
    String APPLICATION_AUTH_NAME    = "ferry-auth";
    String APPLICATION_SYSTEM_NAME  = "ferry-system";
    String APPLICATION_DEVOPS_NAME  = "ferry-devops";

    String  TENANT_ID         = "Tenant-Id";
    String  DEFAULT_TENANT    = "000000";
    String  DEFAULT_PASSWORD  = "123456";
    Integer NOT_DELETED       = IntConst.ZERO;
    Integer IS_DELETED        = IntConst.ONE;
    Integer NOT_DISABLE       = IntConst.ONE;
    Integer IS_DISABLE        = IntConst.ZERO;
    Long    TOP_PARENT_ID     = 0L;
    String  SUPER_ADMIN_CODE  = "admin";
    Integer SUPER_ADMIN_VALUE = IntConst.ONE;

    String GATEWAY_CACHE_PREFIX = APPLICATION_GATEWAY_NAME + StrConst.COLON;
    String AUTH_CACHE_PREFIX    = APPLICATION_AUTH_NAME + StrConst.COLON;
    String SYSTEM_CACHE_PREFIX  = APPLICATION_SYSTEM_NAME + StrConst.COLON;
}
