package com.lx.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public class OrderConfirmVo {

    @Setter @Getter
    List<MemberAddressVo> address;

    @Setter @Getter
    List<OrderItemVo> items;

    //发票信息

    //优惠券信息
    @Setter @Getter
    Integer integration;

    //订单总额
    BigDecimal total;

    //放重令牌
    @Setter @Getter
    String orderToken;

    @Setter @Getter
    Map<Long,Boolean> stocks;

    public Integer getCount(){
        Integer i=0;
        if(items!=null){
            for (OrderItemVo item : items) {
             i+=item.getCount();
            }
        }
        return i;
    }

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(items!=null){
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }

        return sum;
    }

    //应付总额
    BigDecimal payPrice;

    public BigDecimal getPayPrice() {
        BigDecimal sum = new BigDecimal("0");
        if(items!=null){
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }

        return sum;
    }
}
