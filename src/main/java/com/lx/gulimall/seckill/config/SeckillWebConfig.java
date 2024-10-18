package com.lx.gulimall.seckill.config;

import com.lx.gulimall.seckill.interceptor.LoginUserinterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SeckillWebConfig implements WebMvcConfigurer {

    @Autowired
    LoginUserinterceptor loginUserinterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginUserinterceptor).addPathPatterns("/**");
    }
}
