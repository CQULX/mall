package com.lx.gulimall.product.dao;

import com.lx.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 16:43:31
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
