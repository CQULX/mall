package com.lx.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.member.entity.MemberEntity;
import com.lx.gulimall.member.exception.PhoneExistException;
import com.lx.gulimall.member.exception.UsernameExistException;
import com.lx.gulimall.member.vo.MemberLoginVo;
import com.lx.gulimall.member.vo.MemberRegistVo;
import com.lx.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 21:54:29
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo registVo);

    void checkUserNameUnique(String userName) throws UsernameExistException;

    void checkMobileUnique(String phone) throws PhoneExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

