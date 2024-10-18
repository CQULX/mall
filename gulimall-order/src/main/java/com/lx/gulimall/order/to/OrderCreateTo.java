package com.lx.gulimall.order.to;

import com.lx.gulimall.order.entity.OrderEntity;
import com.lx.gulimall.order.entity.OrderItemEntity;
import com.lx.gulimall.order.vo.OrderItemVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {
    private OrderEntity order;
    private List<OrderItemEntity> orderItems;
    private BigDecimal payPrice;//订单计算的应付价格
    private BigDecimal fare;




}
