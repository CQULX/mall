package com.lx.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.lx.common.constant.AuthServerConstant;
import com.lx.common.exception.BizCodeEnume;
import com.lx.common.utils.R;
import com.lx.common.vo.MemberRespVo;
import com.lx.gulimall.auth.feign.MemberFeignService;
import com.lx.gulimall.auth.feign.ThirdPartyFeignService;
import com.lx.gulimall.auth.vo.UserLoginVo;
import com.lx.gulimall.auth.vo.UserRegistVo;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.math.raw.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.naming.Binding;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        //TODO 接口防刷
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis()-l<60*1000){
                //60s 内不能再发
                return R.error(BizCodeEnume.VALID_SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.VALID_SMS_CODE_EXCEPTION.getMsg());
            }

        }



        //验证码的再次校验
        String code = (Math.random()*1000000+"").substring(0, 6);
        //redis缓存验证码
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,code+"_"+System.currentTimeMillis(),10, TimeUnit.MINUTES);

        thirdPartyFeignService.sendCode(phone,code);
        return R.ok();
    }

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, Model model, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){

//            result.getFieldErrors().stream().map(fieldError->{
//                String field = fieldError.getField();
//                String defaultMessage = fieldError.getDefaultMessage();
//                errors.put(field,defaultMessage);
//            })

            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField
            , FieldError::getDefaultMessage));
//            model.addAttribute("errors",errors);
            redirectAttributes.addFlashAttribute("errors",errors);
            //如果校验出错，转发到注册页,
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //真正注册，调用远程服务进行注册
        String code = vo.getCode();
        String s = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            if(code.equals(s.split("_")[0])){
                //删除验证码 令牌机制
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX+vo.getPhone());
                //验证码通过 调用远程服务进行注册
                R regist = memberFeignService.regist(vo);
                if(regist.getCode()==0){

                    return "redirect:http://auth.gulimall.com/login.html";
                }else {
                    Map<String, String> errors =new HashMap<>();
                    errors.put("msg",regist.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }

            }else {
                Map<String, String> errors =new HashMap<>();
                errors.put("code","验证码错误");
//            model.addAttribute("errors",errors);
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else {
            Map<String, String> errors =new HashMap<>();
            errors.put("code","验证码错误");
//            model.addAttribute("errors",errors);
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }


    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute==null){
            //没登录
            return "login";
        }else {
            return "redirect:http://gulimall.com";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        R login = memberFeignService.login(vo);
        if(login.getCode()==0) {
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        }else {
            Map<String,String> errors=new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

        //远程登陆

    }

}
