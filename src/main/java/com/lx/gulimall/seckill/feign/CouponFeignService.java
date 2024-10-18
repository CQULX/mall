package com.lx.gulimall.seckill.feign;

import com.lx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    @GetMapping("coupon/seckillsession/latest3DaySession")
    R getLatest3DaySession();
}
