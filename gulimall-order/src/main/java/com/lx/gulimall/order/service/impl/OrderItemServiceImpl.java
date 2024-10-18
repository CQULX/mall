package com.lx.gulimall.order.service.impl;

import com.lx.gulimall.order.entity.OrderEntity;
import com.lx.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.order.dao.OrderItemDao;
import com.lx.gulimall.order.entity.OrderItemEntity;
import com.lx.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }


    /***
     * 声明需要监听的所有队列
     */
   @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) throws InterruptedException {

        System.out.println("接收到消息...内容："+content);
       long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if(deliveryTag%2==0){
                channel.basicAck(deliveryTag,false);
                System.out.println("签收了货物..."+deliveryTag);
            }else{
                channel.basicNack(deliveryTag,false,true);
            }

        }catch (Exception e){
            //网络中断
        }


    }

    @RabbitHandler
    public void receiveMessage(OrderEntity content) throws InterruptedException {

        System.out.println("接收到消息...内容："+content);


    }
}