package com.lx.gulimall.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lx.gulimall.search.entity.BankEntity;
import com.lx.gulimall.search.entity.Product;
import io.swagger.models.auth.In;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
class GulimallSearchApplicationTests {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	class User{
		private String userName;
		private String gender;
		private Integer age;
	}



	@Autowired
	private ElasticsearchClient client;

	@Test
	void testData(){
		String s="3_";
		String[] s1 = s.split("_");
		System.out.println("s1长度: "+s1.length);
		System.out.println(s1[0]);
//		System.out.println(s1[1]);

	}


	@Test
	void searchData() throws IOException {
		SearchResponse<BankEntity> search1 = client.search(s -> s
						.index("bank_account")
						.query(q -> q
								.term(t -> t
										.field("address")
										.value(v -> v.stringValue("mill"))
								)).aggregations("ageAgg",a->a.terms(t->t.field("age").size(10)))
						.aggregations("ageAvg",a->a.avg(b->b.field("balance"))),
				BankEntity.class);
		System.out.println("search1 = " + search1);
		for (Hit<BankEntity> hit: search1.hits().hits()) {
			BankEntity pd = hit.source();
			System.out.println(pd);
		}
		Aggregate ageAgg = search1.aggregations().get("ageAgg");
		System.out.println("ageAgg = " + ageAgg);
		List<LongTermsBucket> array1 = ageAgg.lterms().buckets().array();
		System.out.println("array1 = " + array1);
		Aggregate ageAvg = search1.aggregations().get("ageAvg");
		System.out.println("ageAvg = " + ageAvg);
	}

	@Test
	void indexData() throws IOException {
//		User user=new User();
//		user.setUserName("胡行");
//		user.setAge(22);
//		user.setGender("男");
		Product product1 = new Product();
		product1.setId("abc");
		product1.setName("Bag");
		product1.setPrice(42);
		IndexRequest<Product> request = IndexRequest.of(i -> i
				.index("products")
				.id(product1.getId())
				.document(product1)
		);
		IndexResponse response=client.index(request);
		System.out.println("response = " + response);
	}

	@Test
	void contextLoads() {
		System.out.println(client);
	}

}
