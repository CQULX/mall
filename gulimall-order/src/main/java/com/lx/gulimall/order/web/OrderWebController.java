package com.lx.gulimall.order.web;

import com.lx.gulimall.order.service.OrderService;
import com.lx.gulimall.order.vo.OrderConfirmVo;
import com.lx.gulimall.order.vo.OrderSubmitVo;
import com.lx.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        System.out.println("进入结算页");
        OrderConfirmVo orderConfirmVo=orderService.confirmOrder();
        model.addAttribute("orderConfirmData",orderConfirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        SubmitOrderResponseVo responseVo=orderService.submitOrder(vo);
        if(responseVo.getCode()==0){
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }else {
            String msg="下单失败；";
            switch (responseVo.getCode()){
                case 1: msg+="订单信息过期，请刷新再次提交"; break;
                case 2: msg+="订单价格商品发生变化，请确认后再提交";break;
                case 3: msg+="库存锁定失败，商品库存不足";break;

            }
            redirectAttributes.addAttribute("msg",msg);
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
