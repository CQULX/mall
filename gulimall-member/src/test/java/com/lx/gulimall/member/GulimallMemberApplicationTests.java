package com.lx.gulimall.member;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.bouncycastle.crypto.Digest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class GulimallMemberApplicationTests {

    @Test
    void contextLoads() {
        String s = DigestUtils.md5Hex("123456");
        System.out.println("s = " + s);
        //盐值加密:随机值
//        String s1 = Md5Crypt.md5Crypt("123456".getBytes());
        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        System.out.println("encode = " + encode);

    }

}
