package com.lx.gulimall.seckill.interceptor;

import com.lx.common.constant.AuthServerConstant;
import com.lx.common.vo.MemberRespVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginUserinterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser=new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/kill", uri);
        if(match){
            MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
            if(attribute!=null){
                loginUser.set(attribute);
                return true;
            }else {
                request.getSession().setAttribute("msg","请先进行登录");
                response.sendRedirect("http://auth.gulimall.com/login.html");
                return false;
            }
        }
        return true;

    }
}
