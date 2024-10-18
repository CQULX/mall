package com.lx.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lx.common.utils.PageUtils;
import com.lx.gulimall.product.entity.CategoryEntity;
import com.lx.gulimall.product.vo.Catelog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 16:43:31
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> list);

    Long[] findCatelogPath(Long catelogId);

    void updateDetail(CategoryEntity category);

    List<CategoryEntity> getLevel1Categorys();

    Map<String, List<Catelog2Vo>> getCatalogJson();
}

