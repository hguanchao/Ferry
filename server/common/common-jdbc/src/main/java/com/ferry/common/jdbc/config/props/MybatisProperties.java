package com.ferry.common.jdbc.config.props;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis Properties属性类
 */
@Data
@ConfigurationProperties(prefix = "ferry.mybatis-plus")
public class MybatisProperties {

    /**
     * 分页最大数
     */
    private Long maxLimit = 100L;

    /**
     * 数据库类型，取值见 {@link DbType}，默认 MySQL
     */
    private DbType dbType = DbType.MYSQL;

    /**
     * 溢出总页数后是否进行处理
     */
    private Boolean overflow = false;

    /**
     * 租户配置
     */
    private Tenant tenant = new Tenant();

    /**
     * 数据权限配置
     */
    private DataPermission dataPermission = new DataPermission();

    @Data
    public static class Tenant {
        /**
         * 租户字段名称
         */
        private String column = "tenant_id";

        /**
         * 要添加租户的表
         */
        private List<String> tables = new ArrayList<>();
    }

    @Data
    public static class DataPermission {
        /**
         * 部门字段名称
         */
        private String deptColumn = "dept_id";

        /**
         * 创建人字段名称
         */
        private String userColumn = "create_user";
    }

}
