package com.lx.gulimall.product.vo;

import com.lx.gulimall.product.entity.SkuImagesEntity;
import com.lx.gulimall.product.entity.SkuInfoEntity;
import com.lx.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {
    SkuInfoEntity info;

   boolean hasStock=true;

    List<SkuImagesEntity> images;

    List<SkuItemSaleAttrVo> saleAttr;

    SpuInfoDescEntity desp;

    List<SpuItemAttrGroupVo> groupAttrs;

    //当前商品的秒杀优惠信息
    SeckillInfoVo seckillInfo;

}
