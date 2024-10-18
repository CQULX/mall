package com.lx.gulimall.auth.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserRegistVo {
    @NotEmpty(message = "用户名必须提交")
    @Length(min = 6,max = 18,message = "用户名必须是6-18位字符")
    private String userName;
    @Length(min = 6,max = 18,message = "密码必须是6-18位字符")
    private String password;
    @NotEmpty(message = "密码必须填写")
    @Pattern(regexp = "^[1]([(3-9)])[0-9]{9}$",message = "手机号格式不正确")
    private String phone;
    @NotEmpty(message = "验证码必须填写")
    private String code;
}
