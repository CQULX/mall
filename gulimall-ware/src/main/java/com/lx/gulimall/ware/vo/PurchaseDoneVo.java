package com.lx.gulimall.ware.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseDoneVo {
    @NotNull
    private Long id;

    private List<PurchaseItemDoneVo> items;
}
