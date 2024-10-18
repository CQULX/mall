package com.lx.gulimall.product.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lx.common.constant.ProductConstant;
import com.lx.common.to.SkuReductionTo;
import com.lx.common.to.SpuBoundTo;
import com.lx.common.to.es.SkuEsModel;
import com.lx.common.utils.R;
import com.lx.gulimall.product.dao.SpuInfoDescDao;
import com.lx.gulimall.product.entity.*;
import com.lx.gulimall.product.feign.CouponFeignService;
import com.lx.gulimall.product.feign.SearchFeignService;
import com.lx.gulimall.product.feign.WareFeignService;
import com.lx.gulimall.product.service.*;
import com.lx.gulimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService descService;

    @Autowired
    SpuImagesService imagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService valueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService saleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        //1、保存Spu基本信息
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        //2、保存Spu的描述图片
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",decript));
        descService.saveSpuInfoDesc(spuInfoDescEntity);
        //3、保存Spu的图片集
        List<String> images = vo.getImages();
        imagesService.saveSpuImages(spuInfoEntity.getId(),images);
        //4、保存Spu的规格参数
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> attrCollect = baseAttrs.stream().map((attr) -> {
            ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
            attrValueEntity.setAttrId(attr.getAttrId());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            attrValueEntity.setAttrName(attrEntity.getAttrName());
            attrValueEntity.setAttrValue(attr.getAttrValues());
            attrValueEntity.setQuickShow(attr.getShowDesc());
            attrValueEntity.setSpuId(spuInfoEntity.getId());
            return attrValueEntity;
        }).collect(Collectors.toList());
        valueService.saveProductAttr(attrCollect);

        //保存Spu的积分信息
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r.getCode()!=0){
            log.error("远程保存spu信息失败");
        }


        //5、保存当前Spu对应的所有Sku信息
        //5.1 Sku基本信息

        List<Skus> skus = vo.getSkus();
        if(skus!=null && skus.size()!=0){
            skus.forEach(item->{
                String defaultImg="";
                for (Images image : item.getImages()) {
                    if(image.getDefaultImg()==1){
                        defaultImg=image.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item,skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());

                skuInfoEntity.setSkuDefaultImg(defaultImg);

                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();


                List<SkuImagesEntity> skuImagesEntities = item.getImages().stream().map((img) -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntities);

                List<Attr> skuAttr = item.getAttr();
                List<SkuSaleAttrValueEntity> saleAttrEntites = skuAttr.stream().map((attr) -> {
                    SkuSaleAttrValueEntity saleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, saleAttrValueEntity);
                    saleAttrValueEntity.setSkuId(skuId);
                    return saleAttrValueEntity;
                }).collect(Collectors.toList());
                saleAttrValueService.saveBatch(saleAttrEntites);

                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                skuReductionTo.setMemberPrice(item.getMemberPrice());
                if(skuReductionTo.getFullCount()>0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0"))==1){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode()!=0){
                        log.error("远程保存sku信息失败");
                    }
                }



            });

        }

        //5.2 Sku的图片信息
        //5.3 Sku的销售属性信息
        //5.4 Sku的优惠、满减信息



    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }
        String status= (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }
        String brandId= (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && Integer.parseInt(brandId)!=0){
            wrapper.eq("brand_id",brandId);
        }
        String catelogId= (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId)&& Integer.parseInt(catelogId)!=0 ){
            wrapper.eq("catalog_id",catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {
        List<SkuEsModel> upProducts=new ArrayList<>();
        //组装所需数据

        //1 查出对应spu的所有sku信息
        List<SkuInfoEntity> skus=skuInfoService.getSkusBySpuId(spuId);

        //查询sku的规格属性
        List<ProductAttrValueEntity> valueEntities = valueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = valueEntities.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<Long> searchAttrIds=attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet=new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrs = valueEntities.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrList = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrList);
            return attrList;
        }).collect(Collectors.toList());

        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());


        Map<Long, Boolean> stockMap=null;
        //TODO:发送远程调用查询库存
        try {
            R skusHasStock = wareFeignService.getSkusHasStock(skuIdList);
//            List<Object> skuHasStock = (List<Object>) skusHasStock.get("data");
//            List<SkuHasStockVo> stockList=new ArrayList<>();
//            for (Object object : skuHasStock) {
//                String jsonObject = JSON.toJSONString(object);
//                SkuHasStockVo skuHasStockVo = JSONObject.parseObject(jsonObject, SkuHasStockVo.class);
//                stockList.add(skuHasStockVo);
//            }
            stockMap=skusHasStock.getData("data",new TypeReference<List<SkuHasStockVo>>(){}).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
            System.out.println("skusHasStock = " + skusHasStock);
        }catch (Exception e){
            log.error("库存服务查询异常:原因{}",e);
        }




        //2 封装sku信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProductInfo = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);

            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            if(finalStockMap ==null){
                esModel.setHasStock(true);
            }else{
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }



            //TODO:热度评分
            esModel.setHotScore(0L);
            BrandEntity brandEntity = brandService.getById(sku.getBrandId());
            esModel.setBrandName(brandEntity.getName());
            esModel.setBrandImg(brandEntity.getLogo());

            CategoryEntity categoryEntity = categoryService.getById(sku.getCatalogId());
            esModel.setCatalogName(categoryEntity.getName());
            esModel.setAttrs(attrs);

            return esModel;
        }).collect(Collectors.toList());

        R r = searchFeignService.productStatusUp(upProductInfo);
        if(r.getCode()==0){
            //修改SPU状态
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else {

        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);
        Long spuId = skuInfoEntity.getSpuId();
        SpuInfoEntity spuInfoEntity = this.getById(spuId);

        return spuInfoEntity;
    }


}