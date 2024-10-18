package com.lx.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.TypeReference;
import com.lx.common.to.es.SkuEsModel;
import com.lx.common.utils.R;
import com.lx.gulimall.search.constant.EsConstant;
import com.lx.gulimall.search.feign.ProductFeignService;
import com.lx.gulimall.search.service.MallSearchService;
import com.lx.gulimall.search.vo.AttrResponseVo;
import com.lx.gulimall.search.vo.BrandVo;
import com.lx.gulimall.search.vo.SearchParam;
import com.lx.gulimall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult result=null;
        SearchRequest request=buildSearchRequest(searchParam);
        try {
            SearchResponse<SkuEsModel> searchResponse = elasticsearchClient.search(request, SkuEsModel.class);
            System.out.println("searchResponse = " + searchResponse);
            //分析响应数据并封装成指定格式
            result=buildSearchResult(searchResponse,searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    //构建结果数据
    private SearchResult buildSearchResult(SearchResponse<SkuEsModel> searchResponse, SearchParam searchParam) {

        SearchResult result = new SearchResult();
        HitsMetadata<SkuEsModel> hits = searchResponse.hits();
        //1 返回所有查询到的商品
        List<SkuEsModel> esModels=new ArrayList<>();
        if(hits.hits()!=null && hits.hits().size()>0){
            for (Hit<SkuEsModel> hit : hits.hits()) {
                SkuEsModel source = hit.source();
                if(searchParam.getKeyword()!=null){
                    String skuTitleHighLight = hit.highlight().get("skuTitle").get(0);
                    source.setSkuTitle(skuTitleHighLight);
                }
                esModels.add(source);
            }
        }
        result.setProducts(esModels);
        //2 当前商品所涉及到的属性信息
        List<SearchResult.AttrVo> attrVos=new ArrayList<>();
        Aggregate attrAgg = searchResponse.aggregations().get("attr_agg");
        Aggregate attrIdAgg = attrAgg.nested().aggregations().get("attr_id_agg");
        List<LongTermsBucket> attrBuckets = attrIdAgg.lterms().buckets().array();
        for (LongTermsBucket attrBucket : attrBuckets) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            Long attrId = attrBucket.key();
            attrVo.setAttrId(attrId);

            Aggregate attrNameAgg = attrBucket.aggregations().get("attr_name_agg");
            List<StringTermsBucket> attrNames = attrNameAgg.sterms().buckets().array();
            String attrName=attrNames.get(0).key().stringValue();
            attrVo.setAttrName(attrName);

            Aggregate attrValueAgg = attrBucket.aggregations().get("attr_value_agg");
            List<StringTermsBucket> attrValuesBucket = attrValueAgg.sterms().buckets().array();
            List<String> attrValues = attrValuesBucket.stream().map(item -> {
                String attrValue = item.key().stringValue();
                return attrValue;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attrValues);
            attrVos.add(attrVo);

        }

        result.setAttrs(attrVos);

        //3 当前商品所涉及到的品牌信息
        List<SearchResult.BrandVo> brandVos=new ArrayList<>();
        Aggregate brandAgg = searchResponse.aggregations().get("brand_agg");
        List<LongTermsBucket> brandBuckets = brandAgg.lterms().buckets().array();
        for (LongTermsBucket brandBucket : brandBuckets) {

            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            Long brandId = brandBucket.key();
            brandVo.setBrandId(brandId);

            Aggregate brandNameAgg = brandBucket.aggregations().get("brand_name_agg");
            List<StringTermsBucket> brandNames = brandNameAgg.sterms().buckets().array();
            String brandName = brandNames.get(0).key().stringValue();
            brandVo.setBrandName(brandName);

            Aggregate brandImgAgg = brandBucket.aggregations().get("brand_img_agg");
            List<StringTermsBucket> brandImgs = brandImgAgg.sterms().buckets().array();
            String brandImg = brandImgs.get(0).key().stringValue();
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);


        //4 当前商品所涉及到的分类信息
        List<SearchResult.CatalogVo> catalogVos=new ArrayList<>();
        Aggregate catalogAgg = searchResponse.aggregations().get("catalog_agg");
        List<LongTermsBucket> catalogBuckets = catalogAgg.lterms().buckets().array();
        for (LongTermsBucket catalogBucket : catalogBuckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            Long catalogId = catalogBucket.key();
            catalogVo.setCatalogId(catalogId);
            //得到分类名字
            Aggregate catalogNameAgg = catalogBucket.aggregations().get("catalog_name_agg");
            List<StringTermsBucket> catalogNames = catalogNameAgg.sterms().buckets().array();
            String catalogName = catalogNames.get(0).key().stringValue();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //5 页码
        result.setPageNum(searchParam.getPageNum());
        //6 总记录数
        long total = hits.total().value();
        result.setTotal(total);
        //7 总页码
        int totalPages=(int)total%EsConstant.PRODUCT_PAGESIZE==0?(int)total/EsConstant.PRODUCT_PAGESIZE:((int)total/EsConstant.PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs=new ArrayList<>();
        for (int i=1;i<=totalPages;i++){
            pageNavs.add(i);
        }
        result.setPageNav(pageNavs);

        //构建面包屑导航功能
        if(searchParam.getAttrs()!=null && searchParam.getAttrs().size()>0){
            List<SearchResult.NavVo> navVos = searchParam.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();

                //封装attr
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if(r.getCode()==0){
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                }else {
                    navVo.setNavName(s[0]);
                }
                //取消了面包屑后需要跳转到的地方，当请求地址的url替换
                String replaceUrl = getReplaceUrl(searchParam, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?"+replaceUrl);
                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }

        //品牌，分类的面包屑导航
        if(searchParam.getBrandId()!=null && searchParam.getBrandId().size()>0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo brandNavVo = new SearchResult.NavVo();
            brandNavVo.setNavName("品牌");
            //TODO 远程查询所有品牌
            R r = productFeignService.brandsInfo(searchParam.getBrandId());
            System.out.println("r = " + r);
            if(r.getCode()==0){
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                System.out.println("brand = " + brand);
                StringBuffer buffer = new StringBuffer();
                String replace="";
                for (BrandVo brandVo : brand) {
                    buffer.append(brandVo.getName()+";");
                    replace=getReplaceUrl(searchParam,brandVo.getBrandId()+"","brandId");
                }
                brandNavVo.setNavValue(buffer.toString());
                brandNavVo.setLink("http://search.gulimall.com/list.html?"+replace);
            }
            navs.add(brandNavVo);
        }




        return result;
    }

    @NotNull
    private static String getReplaceUrl(SearchParam searchParam, String value,String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode=encode.replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replaceUrl;
        if(searchParam.get_queryString().indexOf("?"+key)!=-1){
          replaceUrl = searchParam.get_queryString().replace("?"+key+"=" + encode , "");
        }
        else{
          replaceUrl = searchParam.get_queryString().replace("&"+key+"=" + encode , "");
        }
        return replaceUrl;
    }

    //准备检索请求
    private SearchRequest buildSearchRequest(SearchParam searchParam) {


        //分页
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index(EsConstant.PRODUCT_INDEX);
        requestBuilder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        requestBuilder.size(EsConstant.PRODUCT_PAGESIZE);

//        .query(boolBuilder.build()._toQuery()).sort(sortOptions)
//                .from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE).size(EsConstant.PRODUCT_PAGESIZE)
//                .highlight(highLightBuilder.build());
        BoolQuery.Builder boolBuilder = QueryBuilders.bool();
        //must 模糊匹配
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            Query byKeyword = MatchQuery.of(m->m.field("skuTitle").query(searchParam.getKeyword()))._toQuery();
            boolBuilder.must(byKeyword);
        }
        //filter 三级分类id
        if(searchParam.getCataLog3Id()!=null){
            Query byCatalog= TermQuery.of(t->t.field("catalogId").value(searchParam.getCataLog3Id()))._toQuery();
            boolBuilder.filter(byCatalog);
        }
        //filter 品牌id
        if(searchParam.getBrandId()!=null){
            Query byBrandId=TermsQuery.of(t->t.field("brandId").terms(new TermsQueryField.Builder().value(
                    searchParam.getBrandId().stream().map(FieldValue::of).toList()
            ).build()))._toQuery();
            boolBuilder.filter(byBrandId);
        }

        //按指定的属性进行查询
        if(searchParam.getAttrs()!=null &&searchParam.getAttrs().size()>0 ){
            for (String attrStr : searchParam.getAttrs()) {
                BoolQuery.Builder nestedBoolQueryBuilder = QueryBuilders.bool();
                String[] s = attrStr.split("_");
                String attrId=s[0]; //检索属性id
                String[] s1 = s[1].split(":"); //检索属性值
                List<String> attrValues = Arrays.stream(s1).toList();
                Query byAttrId=TermQuery.of(t->t.field("attrs.attrId").value(attrId))._toQuery();
                Query byAttrValue=TermsQuery.of(t->t.field("attrs.attrValue").terms(new TermsQueryField.Builder().value(
                        attrValues.stream().map(FieldValue::of).toList()
                ).build()))._toQuery();
                nestedBoolQueryBuilder.must(byAttrId);
                nestedBoolQueryBuilder.must(byAttrValue);
                BoolQuery nestedBoolQuery = nestedBoolQueryBuilder.build();
                //每一个都得生成嵌入式的nestedquery
                NestedQuery.Builder attrBuild = QueryBuilders.nested().path("attrs").query(nestedBoolQuery._toQuery()).scoreMode(ChildScoreMode.None);
                boolBuilder.filter(attrBuild.build()._toQuery());
            }

        }


        //按照是否有库存进行查询
        if(searchParam.getHasStock()!=null){
            Query byhasStock=TermQuery.of(t->t.field("hasStock").value(searchParam.getHasStock()==1))._toQuery();
            boolBuilder.filter(byhasStock);
        }



        //按照价格区间
        if(!StringUtils.isEmpty(searchParam.getSkuPrice())){
            RangeQuery.Builder rangeBuild = QueryBuilders.range().field("skuPrice");
            String[] s = searchParam.getSkuPrice().split("_");
            if(s.length==2){
                if(searchParam.getSkuPrice().startsWith("_")){
                    rangeBuild.lte(JsonData.of(s[1]));
                }else{
                    rangeBuild.gte(JsonData.of(s[0])).lte(JsonData.of(s[1]));
                }
            }else if (s.length==1){
                rangeBuild.gte(JsonData.of(s[0]));
            }
            boolBuilder.filter(rangeBuild.build()._toQuery());
        }

        requestBuilder.query(boolBuilder.build()._toQuery());

        //排序
        List<SortOptions> sortOptions = null;
        if(!StringUtils.isEmpty(searchParam.getSort())){
            String sort = searchParam.getSort();
            String[] s = sort.split("_");
            SortOptions.Builder sortBuilder = new SortOptions.Builder();
            SortOrder order=s[1].equalsIgnoreCase("asc")?SortOrder.Asc:SortOrder.Desc;
            sortBuilder.field(f-> f.field(s[0]).order(order));
            SortOptions sortOption = sortBuilder.build();
            sortOptions = Arrays.asList(sortOption);
            requestBuilder.sort(sortOption);
        }




        //高亮
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            requestBuilder.highlight(h->h.fields("skuTitle",f->f.preTags("<b style='color:red'>").postTags("</b>")));
        }


        //聚合分析


        //品牌聚合
        requestBuilder.aggregations("brand_agg",a->a.terms(t->t.field("brandId").size(50))
                      .aggregations("brand_name_agg",a1->a1.terms(t1->t1.field("brandName").size(1)))
                      .aggregations("brand_img_agg",a2->a2.terms(t2->t2.field("brandImg").size(1))));

        //分类聚合

        requestBuilder.aggregations("catalog_agg",a->a.terms(t->t.field("catalogId").size(20))
                      .aggregations("catalog_name_agg",a1->a1.terms(t1->t1.field("catalogName").size(1))));


        //属性聚合
        requestBuilder.aggregations("attr_agg",a->a.nested(n->n.path("attrs"))
                      .aggregations("attr_id_agg",a1->a1.terms(t1->t1.field("attrs.attrId"))
                              .aggregations("attr_name_agg",a11->a11.terms(t11->t11.field("attrs.attrName").size(1)))
                              .aggregations("attr_value_agg",a12->a12.terms(t12->t12.field("attrs.attrValue").size(50)))));

        SearchRequest request = requestBuilder.build();
        String string = request.toString();
        System.out.println("构建的DSL= " + string);

        return request;
    }
}
