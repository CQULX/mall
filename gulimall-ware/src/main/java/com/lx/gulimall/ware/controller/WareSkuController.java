package com.lx.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.lx.common.exception.BizCodeEnume;
import com.lx.common.exception.NoStockException;
import com.lx.gulimall.ware.vo.SkuHasStockVo;
import com.lx.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lx.gulimall.ware.entity.WareSkuEntity;
import com.lx.gulimall.ware.service.WareSkuService;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.R;



/**
 * 商品库存
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 22:31:05
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo){
        try {
            Boolean stockResults=wareSkuService.orderLockStock(vo);
            return R.ok();
        }catch (NoStockException e){
          return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(), BizCodeEnume.NO_STOCK_EXCEPTION.getMsg());
        }



    }

    @PostMapping("/hasstock")
    //查询是否有库存
    public R getSkusHasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStockVo> vos=wareSkuService.getSkusHasStock(skuIds);
        return R.ok().put("data",vos);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
   // @RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
