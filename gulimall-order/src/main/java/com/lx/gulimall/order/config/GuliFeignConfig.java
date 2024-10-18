package com.lx.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class GuliFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor(){
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(requestAttributes!=null){
                    HttpServletRequest request = requestAttributes.getRequest();//老请求
                    //同步请求头数据 (同步cookie)
                    if(request!=null){
                        String cookie = request.getHeader("Cookie");
                        template.header("Cookie",cookie);
                    }
                }
            }
        };
    }
}
