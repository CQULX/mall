package com.lx.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResult {
    private Long skuId;
    private Integer number;
    private Boolean locked;
}
