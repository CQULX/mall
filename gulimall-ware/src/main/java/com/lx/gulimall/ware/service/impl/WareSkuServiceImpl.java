package com.lx.gulimall.ware.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.TypeReference;
import com.lx.common.exception.NoStockException;
import com.lx.common.to.mq.OrderTo;
import com.lx.common.to.mq.StockDetailTo;
import com.lx.common.to.mq.StockLockedTo;
import com.lx.common.utils.R;
import com.lx.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.lx.gulimall.ware.entity.WareOrderTaskEntity;
import com.lx.gulimall.ware.feign.OrderFeignService;
import com.lx.gulimall.ware.feign.ProductFeignService;
import com.lx.gulimall.ware.service.WareOrderTaskDetailService;
import com.lx.gulimall.ware.service.WareOrderTaskService;
import com.lx.gulimall.ware.vo.OrderItemVo;
import com.lx.gulimall.ware.vo.OrderVo;
import com.lx.gulimall.ware.vo.SkuHasStockVo;
import com.lx.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.ware.dao.WareSkuDao;
import com.lx.gulimall.ware.entity.WareSkuEntity;
import com.lx.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService orderTaskService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;


    private void unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId){

        wareSkuDao.unlockStock(skuId,wareId,num);
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
        wareOrderTaskDetailEntity.setId(taskDetailId);
        wareOrderTaskDetailEntity.setLockStatus(2);
        orderTaskDetailService.updateById(wareOrderTaskDetailEntity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuid = (String) params.get("skuid");
        if(!StringUtils.isEmpty(skuid)){
            queryWrapper.eq("sku_id",skuid);
        }
        String wareid = (String) params.get("wareid");
        if(!StringUtils.isEmpty(wareid)){
            queryWrapper.eq("ware_id",wareid);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStore(Long skuId, Long wareId, Integer skuNum) {
        //1、如果没有库存记录（新增）
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(wareSkuEntities.size()==0 || wareSkuEntities==null){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);

            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch(Exception e){

                }


            wareSkuDao.insert(wareSkuEntity);
        }else {
            wareSkuDao.addStore(skuId,wareId,skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> hasStockVos = skuIds.stream().map(sku -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId(sku);
            Long count=this.baseMapper.getSkuStock(sku);
            vo.setHasStock(count==null?false:count>0);
            return vo;
        }).collect(Collectors.toList());
        return hasStockVos;
    }

    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //按照下单的收货地址，找到一个就近仓库，锁定库存

        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());

        orderTaskService.save(wareOrderTaskEntity);

        //找到哪个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();

            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            //查询商品在哪里有库存
            List<Long> wareIds=wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            stock.setNum(item.getCount());
            return stock;
        }).collect(Collectors.toList());
        for (SkuWareHasStock skuWareHasStock : collect) {
            Boolean skuStocked=false;
            Long skuId = skuWareHasStock.getSkuId();
            List<Long> wareIds = skuWareHasStock.getWareId();
            if(wareIds==null ||wareIds.size()==0){
                //没有任何仓库有这个商品的库存
                throw new  NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功返回1 否则返回0
                Long count=wareSkuDao.lockSkuStock(skuId,wareId,skuWareHasStock.getNum());
                if(count==1){
                    skuStocked=true;
                    //TODO 告诉MQ锁定成功
                    WareOrderTaskDetailEntity wareOrderTaskDetail = new WareOrderTaskDetailEntity(null, skuId, "", skuWareHasStock.getNum(), wareOrderTaskEntity.getId(), wareId, 1);

                    orderTaskDetailService.save(wareOrderTaskDetail);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetail,stockDetailTo);
                    stockLockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",stockLockedTo);
                    break;
                }else {

                }
            }
            if(skuStocked==false){
                //当前商品所有商品都无库存
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo stockLockedTo) {

        StockDetailTo detail = stockLockedTo.getDetail();
        Long detailId = detail.getId();
        Long skuId = detail.getSkuId();
        //查询数据库有关这个订单的锁库存消息
        //有 需要解锁
        //没有 库存锁定失败，库存回滚，不用解锁
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if(byId!=null){
            //解锁
            Long id = stockLockedTo.getId();
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            if(r.getCode()==0){
                //订单数据返回成功
                OrderVo data = r.getData("data", new TypeReference<OrderVo>() {
                   });

                if(data==null || data.getStatus()==4){
                    //订单已经被取消了 解锁库存

                    if(byId.getLockStatus()==1){
                        unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detailId);
                    }


                }else{
                    //消息拒绝后重新放到队列
                    throw new RuntimeException("远程服务失败");
                }
            }
        }else{
            //无需解锁
            }
    }

    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下最新状态
//        R r = orderFeignService.getOrderStatus(orderSn);
        //查一下最新库存解锁状态
        WareOrderTaskEntity task=orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long taskId = task.getId();
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", taskId)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }


    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}