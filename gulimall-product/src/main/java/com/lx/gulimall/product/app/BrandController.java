package com.lx.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.lx.common.valid.AddGroup;
import com.lx.common.valid.UpdateGroup;
import com.lx.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.lx.gulimall.product.entity.BrandEntity;
import com.lx.gulimall.product.service.BrandService;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.R;



/**
 * 品牌
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 17:21:04
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    @GetMapping("/infos")
    public R infos(@RequestParam("brandIds") List<Long> brandIds){
        List<BrandEntity> brand=brandService.getBrandsByIds(brandIds);
        return R.ok().put("brand", brand);
    }


    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand /*, BindingResult result */){
//		if(result.hasErrors()){
//            Map<String,String> map=new HashMap<>();
//            result.getFieldErrors().forEach((item)->{
//                String defaultMessage = item.getDefaultMessage();
//                String field = item.getField();
//                map.put(field,defaultMessage);
//            });
//
//            return R.error(400,"提交的数据不合法").put("data",map);
//        }else{
//            brandService.save(brand);
//        }

        brandService.save(brand);
        return R.ok();
    }

    /**
     * 修改
     */
    @Transactional
    @RequestMapping("/update")
   // @RequiresPermissions("product:brand:update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }

    @RequestMapping("/update/status")
    // @RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated(UpdateStatusGroup.class) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
