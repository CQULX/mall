package com.lx.gulimall.ware.service.impl;

import com.lx.common.constant.WareConstant;
import com.lx.gulimall.ware.entity.PurchaseDetailEntity;
import com.lx.gulimall.ware.service.PurchaseDetailService;
import com.lx.gulimall.ware.service.WareSkuService;
import com.lx.gulimall.ware.vo.MergeVo;
import com.lx.gulimall.ware.vo.PurchaseDoneVo;
import com.lx.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.ware.dao.PurchaseDao;
import com.lx.gulimall.ware.entity.PurchaseEntity;
import com.lx.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService detailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if(purchaseId==null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId=purchaseEntity.getId();
        }

        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;

        List<PurchaseDetailEntity> purchaseDetailEntities = items.stream().map(item -> {
            PurchaseDetailEntity byId = detailService.getById(item);
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(byId.getStatus());
            return purchaseDetailEntity;
        }).filter(item->{
            if(item.getStatus()==WareConstant.PurchaseDetailStatusEnum.CREATED.getCode()||
            item.getStatus()==WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode()){
                return true;
            }
            return false;
        }).map(item->{
            item.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return item;
        }).collect(Collectors.toList());
        detailService.updateBatchById(purchaseDetailEntities);
        PurchaseEntity purchaseEntityUpdate = new PurchaseEntity();
        purchaseEntityUpdate.setId(purchaseId);
        purchaseEntityUpdate.setUpdateTime(new Date());
        this.updateById(purchaseEntityUpdate);
    }

    @Override
    public void received(List<Long> ids) {
        //1、确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> purchaseEntities = ids.stream().map(id -> {
            PurchaseEntity purchaseEntity = this.getById(id);
            return purchaseEntity;
        }).filter(item->{
            if (    item.getStatus()== WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus()==WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item->{
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVED.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());


        //2、改变采购单的状态

        this.updateBatchById(purchaseEntities);

        //3、改变采购项的状态
        purchaseEntities.forEach((item)->{
            List<PurchaseDetailEntity> detailEntities=detailService.listDetailByPurchaseId(item.getId());
            detailEntities.forEach(entity->{
                entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
            });
            detailService.updateBatchById(detailEntities);
        });
    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {


        //1、改变采购项状态
        List<PurchaseDetailEntity> updates=new ArrayList<>();
        Boolean flag=true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            if(item.getStatus()==WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag=false;
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
            }else{
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISHED.getCode());
                //3、采购成功的商品入库
                PurchaseDetailEntity detailEntity = detailService.getById(item.getItemId());
                wareSkuService.addStore(detailEntity.getSkuId(),detailEntity.getWareId(),detailEntity.getSkuNum());
            }
            purchaseDetailEntity.setId(item.getItemId());
            updates.add(purchaseDetailEntity);
        }
        detailService.updateBatchById(updates);

        //2、改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(doneVo.getId());
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISHED.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);




    }

}