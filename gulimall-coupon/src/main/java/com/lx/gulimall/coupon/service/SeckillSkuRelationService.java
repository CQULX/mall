package com.lx.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.coupon.entity.SeckillSkuRelationEntity;

import java.util.Map;

/**
 * 秒杀活动商品关联
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 21:35:22
 */
public interface SeckillSkuRelationService extends IService<SeckillSkuRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

