package com.lx.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.member.entity.MemberStatisticsInfoEntity;

import java.util.Map;

/**
 * 会员统计信息
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 21:54:29
 */
public interface MemberStatisticsInfoService extends IService<MemberStatisticsInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

