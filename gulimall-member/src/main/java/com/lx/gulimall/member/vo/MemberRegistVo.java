package com.lx.gulimall.member.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class MemberRegistVo {

    private String userName;

    private String password;

    private String phone;
}
