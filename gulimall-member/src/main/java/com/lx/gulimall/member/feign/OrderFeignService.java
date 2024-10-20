package com.lx.gulimall.member.feign;

import com.lx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient("gulimall-order")
public interface OrderFeignService {

    @PostMapping("/order/order/listWithItem")
    //@RequiresPermissions("order:order:list")
     R listWithItem(@RequestBody Map<String, Object> params);
}
