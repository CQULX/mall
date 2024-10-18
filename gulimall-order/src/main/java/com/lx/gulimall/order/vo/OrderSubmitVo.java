package com.lx.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/***
 * 封装订单提交的数据
 */

@Data
public class OrderSubmitVo {
    private Long addrId;//收货地址的id
    private Integer payType;//支付方式
    //无需提交购买的商品
    private String orderToken;//放重令牌
    private BigDecimal payPrice;
    private String note;//订单备注

}
