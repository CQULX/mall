package com.lx.gulimall.order.vo;

import com.lx.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {
    private OrderEntity order;
    private Integer code;//错误状态码
}
