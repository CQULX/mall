package com.lx.gulimall.auth.feign;

import com.lx.common.exception.BizCodeEnume;
import com.lx.common.utils.R;
import com.lx.gulimall.auth.vo.SocialUser;
import com.lx.gulimall.auth.vo.UserLoginVo;
import com.lx.gulimall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
     R regist(@RequestBody UserRegistVo registVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUser socialUser) throws Exception;
}
