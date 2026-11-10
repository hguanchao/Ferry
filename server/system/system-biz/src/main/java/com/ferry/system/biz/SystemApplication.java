package com.ferry.system.biz;

import com.ferry.common.core.constant.SysConst;
import com.ferry.common.web.annotation.CloudApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 系统管理服务启动类
 */
@CloudApplication
@MapperScan("com.ferry.system.biz.mapper")
@EnableFeignClients({SysConst.BASE_PACKAGES})
public class SystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
