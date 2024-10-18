package com.lx.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.lx.common.to.es.SkuEsModel;
import com.lx.gulimall.search.constant.EsConstant;
import com.lx.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    ElasticsearchClient elasticsearchClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {

        //将数据保存到ES中

        //1 es中建立索引,建立好映射关系

        //2 es中保存数据
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (SkuEsModel skuEsModel : skuEsModels) {
            br.operations(op->op.index(idx->idx
                    .index(EsConstant.PRODUCT_INDEX)
                    .id(skuEsModel.getSkuId().toString())
                    .document(skuEsModel)
                )
            );
        }

        BulkResponse result = elasticsearchClient.bulk(br.build());
        boolean errors = result.errors();
        List<BulkResponseItem> items = result.items();
        List<String> collect = items.stream().map(item -> {
            return item.id();
        }).collect(Collectors.toList());

        log.info("商品上架成功:{}",collect);
        return errors;
    }
}
