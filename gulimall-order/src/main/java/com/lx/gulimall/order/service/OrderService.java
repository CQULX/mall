package com.lx.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.to.mq.SeckillOrderTo;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.order.entity.OrderEntity;
import com.lx.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 22:04:05
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);

    void createSeckillOrder(SeckillOrderTo seckillOrder);
}

