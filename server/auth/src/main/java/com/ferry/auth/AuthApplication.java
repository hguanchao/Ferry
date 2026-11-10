package com.ferry.auth;

import com.ferry.common.core.constant.SysConst;
import com.ferry.common.web.annotation.CloudApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@CloudApplication
@EnableFeignClients({SysConst.BASE_PACKAGES})
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
