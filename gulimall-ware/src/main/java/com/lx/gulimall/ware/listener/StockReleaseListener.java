package com.lx.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.lx.common.to.mq.OrderTo;
import com.lx.common.to.mq.StockDetailTo;
import com.lx.common.to.mq.StockLockedTo;
import com.lx.common.utils.R;
import com.lx.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.lx.gulimall.ware.entity.WareOrderTaskEntity;
import com.lx.gulimall.ware.service.WareSkuService;
import com.lx.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "stock.release.stock.queue")
@Service
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handlStockLockedRelease(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存消息");
        try{
            wareSkuService.unlockStock(stockLockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }

    @RabbitHandler
    public void handleOrderRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭，准备解锁库存");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }


}
