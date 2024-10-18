package com.lx.gulimall.product.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.TypeReference;
import com.lx.common.utils.R;
import com.lx.gulimall.product.entity.SkuImagesEntity;
import com.lx.gulimall.product.entity.SpuInfoDescEntity;
import com.lx.gulimall.product.feign.SeckillFeignService;
import com.lx.gulimall.product.service.*;
import com.lx.gulimall.product.vo.SeckillInfoVo;
import com.lx.gulimall.product.vo.SkuItemSaleAttrVo;
import com.lx.gulimall.product.vo.SkuItemVo;
import com.lx.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.product.dao.SkuInfoDao;
import com.lx.gulimall.product.entity.SkuInfoEntity;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService saleAttrValueService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    SeckillFeignService seckillFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w->{
               w.eq("sku_id",key).or().like("sku_name",key);
            });
        }
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&& Integer.parseInt(brandId)!=0){
            wrapper.eq("brand_id",brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) &&Integer.parseInt(catelogId)!=0){
            wrapper.eq("catalog_id",catelogId);
        }
        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            wrapper.ge("price",min);
        }
        String max = (String) params.get("max");
        if(!StringUtils.isEmpty(max)){
            try{
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0"))==1){
                    wrapper.le("price",max);
                }
            }catch (Exception e){

            }

        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> skuInfoEntities = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return skuInfoEntities;
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        //Sku基本信息
        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //spu销售属性组合
            Long spuId = res.getSpuId();
            List<SkuItemSaleAttrVo> saleAttrVos = saleAttrValueService.getSaleAttrsBySpuId(spuId);
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            //spu介绍
            Long spuId = res.getSpuId();
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
            skuItemVo.setDesp(spuInfoDesc);
        }, executor);

        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //spu规格参数信息
            Long spuId = res.getSpuId();
            Long catalogId = res.getCatalogId();
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);

        //sku图片信息
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);


        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            //查询当前sku是否参与秒杀优惠
            R seckillInfo = seckillFeignService.getSkuSeckillInfo(skuId);
            if (seckillInfo.getCode() == 0) {
                SeckillInfoVo seckillInfoData = seckillInfo.getData("data", new TypeReference<SeckillInfoVo>() {
                });
                skuItemVo.setSeckillInfo(seckillInfoData);
            }
        }, executor);


        //等待所有任务都完成
        CompletableFuture.allOf(saleAttrFuture,descFuture,baseAttrFuture,imageFuture,seckillFuture).get();

        return skuItemVo;
    }

}