package com.lx.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.member.entity.MemberCollectSubjectEntity;

import java.util.Map;

/**
 * 会员收藏的专题活动
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 21:54:29
 */
public interface MemberCollectSubjectService extends IService<MemberCollectSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

