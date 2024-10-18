package com.lx.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.to.mq.OrderTo;
import com.lx.common.to.mq.StockLockedTo;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.ware.entity.WareSkuEntity;
import com.lx.gulimall.ware.vo.SkuHasStockVo;
import com.lx.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 22:31:05
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStore(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);


    void unlockStock(StockLockedTo stockLockedTo);

    void unlockStock(OrderTo orderTo);
}

