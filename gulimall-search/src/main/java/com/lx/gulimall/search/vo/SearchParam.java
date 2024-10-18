package com.lx.gulimall.search.vo;

import lombok.Data;

import java.util.List;

//封装页面可能传来的所有查询条件

@Data
public class SearchParam {

    private String keyword;//全文匹配关键字
    private Long cataLog3Id;//三级分类id

    private String sort;//排序条件

    private Integer hasStock;//是否只显示有货
    private String skuPrice;//价格区间
    private List<Long> brandId;
    private List<String> attrs;//按照属性进行筛选
    private Integer pageNum=1;//页码

    private String _queryString; //原生的所有查询条件


}
