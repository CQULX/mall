package com.lx.gulimall.member.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lx.common.utils.HttpUtils;
import com.lx.gulimall.member.dao.MemberLevelDao;
import com.lx.gulimall.member.entity.MemberLevelEntity;
import com.lx.gulimall.member.exception.PhoneExistException;
import com.lx.gulimall.member.exception.UsernameExistException;
import com.lx.gulimall.member.service.MemberLevelService;
import com.lx.gulimall.member.vo.MemberLoginVo;
import com.lx.gulimall.member.vo.MemberRegistVo;
import com.lx.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.member.dao.MemberDao;
import com.lx.gulimall.member.entity.MemberEntity;
import com.lx.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo registVo) {
        MemberEntity memberEntity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity default_status = memberLevelService.getOne(new QueryWrapper<MemberLevelEntity>().eq("default_status", 1L));
        memberEntity.setLevelId(default_status.getId());
        //检查用户名和手机号是否唯一

        checkMobileUnique(registVo.getPhone());
        checkUserNameUnique(registVo.getUserName());



        memberEntity.setMobile(registVo.getPhone());
        memberEntity.setUsername(registVo.getUserName());
        memberEntity.setNickname(registVo.getUserName());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(registVo.getPassword());
        memberEntity.setPassword(encode);

        baseMapper.insert(memberEntity);
    }

    @Override
    public void checkUserNameUnique(String userName) {
        Long count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count>0){
            throw new UsernameExistException();
        }

    }

    @Override
    public void checkMobileUnique(String phone) {
        Long count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count>0){
            throw new PhoneExistException();
        }

    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        //去数据库查询
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct)
                .or().eq("mobile", loginacct));
        if(memberEntity==null){
            return null;
        }else{
            String passwordDb = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //密码匹配
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if(matches){
                return memberEntity;
            }else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //登陆和注册合并逻辑
        Map<String,String> header=new HashMap<>();
        Map<String,String> query=new HashMap<>();

        String accessToken = socialUser.getAccess_token();
        query.put("access_token",accessToken);

        HttpResponse httpResponse = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", header, query);
        String uid="";
        String name="";
        if(httpResponse.getStatusLine().getStatusCode()==200) {
            String json = EntityUtils.toString(httpResponse.getEntity());
            JSONObject jsonObject = JSON.parseObject(json);
            uid = jsonObject.getString("id");
            name = jsonObject.getString("name");
        }
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(memberEntity!=null){
            MemberEntity update=new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(accessToken);
            update.setExpiresIn(socialUser.getExpires_in());

            this.baseMapper.updateById(update);
            return memberEntity;
        }else {
            MemberEntity regist=new MemberEntity();
            regist.setNickname(name);
            regist.setSocialUid(uid);
            regist.setAccessToken(accessToken);
            regist.setExpiresIn(socialUser.getExpires_in());
            this.baseMapper.insert(regist);
            return regist;
        }
    }

}