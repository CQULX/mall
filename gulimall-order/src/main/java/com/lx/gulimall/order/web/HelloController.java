package com.lx.gulimall.order.web;

import com.lx.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.UUID;

@Controller
public class HelloController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable("page") String page){
        return page;
    }

    @ResponseBody
    @GetMapping("/test/createOrder")
    public String createOrderTest(){
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(UUID.randomUUID().toString());
        orderEntity.setModifyTime(new Date());
        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",orderEntity);
        return "ok";
    }

}
