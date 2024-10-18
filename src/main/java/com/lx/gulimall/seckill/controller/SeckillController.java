package com.lx.gulimall.seckill.controller;

import com.lx.common.utils.R;
import com.lx.gulimall.seckill.service.SeckillService;
import com.lx.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SeckillController {


    @Autowired
    SeckillService seckillService;

    /***
     * 返回当前时间可以参与秒杀的商品信息
     * @return
     */
    @ResponseBody
    @GetMapping("currentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SeckillSkuRedisTo> vos=seckillService.getCurrentSeckillSkus();
        return R.ok().put("data",vos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){

        SeckillSkuRedisTo to=seckillService.getSkuSeckillInfo(skuId);
        return R.ok().put("data",to);
    }

    @GetMapping("/kill")
    public String secKill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model) {
        String orderSn=seckillService.kill(killId,key,num);

        model.addAttribute("orderSn",orderSn);
        return "success";
    }

}
