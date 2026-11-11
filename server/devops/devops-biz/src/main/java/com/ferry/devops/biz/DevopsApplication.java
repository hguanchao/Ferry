package com.ferry.devops.biz;

import com.ferry.common.core.constant.SysConst;
import com.ferry.common.web.annotation.CloudApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 交付域服务启动类
 */
@CloudApplication
@MapperScan("com.ferry.devops.biz.mapper")
@EnableFeignClients({SysConst.BASE_PACKAGES})
public class DevopsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevopsApplication.class, args);
    }
}
