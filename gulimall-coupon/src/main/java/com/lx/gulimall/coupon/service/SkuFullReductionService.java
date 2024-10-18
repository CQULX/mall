package com.lx.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.to.SkuReductionTo;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 21:35:22
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

