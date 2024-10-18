package com.lx.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson2.JSON;
import com.lx.common.utils.HttpUtils;
import com.lx.common.utils.R;
import com.lx.gulimall.auth.feign.MemberFeignService;
import com.lx.common.vo.MemberRespVo;
import com.lx.gulimall.auth.vo.SocialUser;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/***
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession httpSession) throws Exception {
        Map<String,String> header=new HashMap<>();
        Map<String,String> query=new HashMap<>();

        //根据code换来access_token
        Map<String,String> map=new HashMap<>();
        map.put("client_id","eafd9115846dc7eb91ca1ce67e8dae7f1cf9c5b6d9d30af2b637575c0b093d9c");
        map.put("client_secret","6e8a1bc4914ed8e5682d257049d55f538262410742bf7a1a0f62b7f3043cb118");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/gitee/success");
        map.put("code",code);

        HttpResponse httpResponse = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", header, query, map);
        if(httpResponse.getStatusLine().getStatusCode()==200){
            String json = EntityUtils.toString(httpResponse.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //知道当前是哪个社交用户 若用户第一次登陆，为当前社交用户生成一个会员信息账号，以后这个社交帐号对应关联指定的会员
            R oauthLogin = memberFeignService.oauthLogin(socialUser);
            if(oauthLogin.getCode()==0){
                MemberRespVo data = oauthLogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                System.out.println("登陆成功，用户信息："+data);
                log.info("登录成功:用户信息{}",data.toString());
                httpSession.setAttribute("loginUser",data);
                return "redirect:http://gulimall.com";
            }else {
                return "redirect:auth.gulimall.com/login.html";
            }

        }else{
            return "redirect:auth.gulimall.com/login.html";
        }
        //登陆成功，跳回首页

    }

}
