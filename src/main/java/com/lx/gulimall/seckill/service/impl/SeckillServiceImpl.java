package com.lx.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lx.common.to.mq.SeckillOrderTo;
import com.lx.common.utils.R;
import com.lx.common.vo.MemberRespVo;
import com.lx.gulimall.seckill.feign.CouponFeignService;
import com.lx.gulimall.seckill.feign.ProductFeignService;
import com.lx.gulimall.seckill.interceptor.LoginUserinterceptor;
import com.lx.gulimall.seckill.service.SeckillService;
import com.lx.gulimall.seckill.to.SeckillSkuRedisTo;
import com.lx.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.lx.gulimall.seckill.vo.SeckillSkuVo;
import com.lx.gulimall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX="seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX="seckill:skus";

    private final String SKU_STOCK_SEMAPHORE="seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1 扫描需要参与秒杀的活动
        R session = couponFeignService.getLatest3DaySession();
        if(session.getCode()==0){
            List<SeckillSessionsWithSkus> sessionData = session.getData("data", new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //1 缓存活动信息
            saveSessionInfos(sessionData);
            //2 缓存活动关联商品信息
            saveSessionSkuInfos(sessionData);

        }
    }

    //@SentinelResource(value="getCurrentSeckillSkus")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        //try(Entry entry=SphU.entry("seckillSkus")){}catch(){}
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            Long start=Long.parseLong(s[0]);
            Long end=Long.parseLong(s[1]);
            if(time>=start && time<=end){
                //2 获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, 0, -1);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if(list!=null){
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo skuRedisTo = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
//                        skuRedisTo.setRandomCode(null);
                        return skuRedisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }


        //3
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        //找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if(keys!=null && keys.size()>0){
            String regx="\\d_"+skuId;
            for (String key : keys) {
                if(Pattern.matches(regx, key)){
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

                    long currentTime = new Date().getTime();
                    Long startTime = skuRedisTo.getStartTime();
                    Long endTime = skuRedisTo.getEndTime();
                    //TODO 不同场次相同商品处理
                    if(currentTime>=startTime && currentTime<=endTime){

                    }else{
                        skuRedisTo.setRandomCode(null);
                    }

                    return skuRedisTo;
                }


            }
        }

        return null;
    }

    @Override
    public String kill(String killId, String key, Integer number) {
        long s1 = System.currentTimeMillis();
        MemberRespVo memberRespVo = LoginUserinterceptor.loginUser.get();

        //获取当前秒杀商品的详情信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if(StringUtils.isEmpty(json)){
            return null;
        }else {
            SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //校验合法性
            Long startTime = skuRedisTo.getStartTime();
            Long endTime = skuRedisTo.getEndTime();
            long curTime = new Date().getTime();
            long ttl=endTime-curTime;
            //校验时间的合法性
            if(curTime>=startTime && curTime<=endTime){
                //校验随机码和商品id是否正确
                String randomCode = skuRedisTo.getRandomCode();
                String skuId = skuRedisTo.getPromotionSessionId()+"_"+skuRedisTo.getSkuId();
                if(randomCode.equals(key) && killId.equals(skuId)){
                    //购物数量是否合理
                    if(number <= skuRedisTo.getSeckillLimit().intValue()){
                        //验证是否已经买过了 幂等性  userId_SessionId_skuId
                        //SETNX
                        String redisKey=memberRespVo.getId()+"_"+killId;
                        //自动过期

                        Boolean isPlace = redisTemplate.opsForValue().setIfAbsent(redisKey, number.toString(), ttl, TimeUnit.MILLISECONDS);
                        if(isPlace){
                            //从来没买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);

                            boolean b = semaphore.tryAcquire(number);
                            //秒杀成功
                            //快速下单，发送MQ消息
                            if(b){
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(timeId);
                                seckillOrderTo.setMemberId(memberRespVo.getId());
                                seckillOrderTo.setNum(number);
                                seckillOrderTo.setPromotionSessionId(skuRedisTo.getPromotionSessionId());
                                seckillOrderTo.setSkuId(skuRedisTo.getSkuId());
                                seckillOrderTo.setSeckillPrice(skuRedisTo.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",seckillOrderTo);
                                long s2 = System.currentTimeMillis();
                                log.info("耗时...{}",(s2-s1));
                                return timeId;
                            }
                            return null;
                            //秒杀成功
                        }else{
                            return null;
                        }
                    }
                }else{
                    return null;
                }
            }else {
                return null;
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions){
        if(sessions!=null){
            sessions.stream().forEach(session->{
                Long startTime = session.getStartTime().getTime();
                Long endTime = session.getEndTime().getTime();
                String key=SESSIONS_CACHE_PREFIX+startTime+"_"+endTime;

                //缓存活动信息
                Boolean hasKey = redisTemplate.hasKey(key);
                if(!hasKey){
                    List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId().toString()+"_"+item.getSkuId().toString()).collect(Collectors.toList());
                    redisTemplate.opsForList().leftPushAll(key,collect);
                }

            });
        }

    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions){
        if(sessions!=null){
            sessions.stream().forEach(session->{
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

                session.getRelationSkus().stream().forEach(seckillSkuVo->{
                    //随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    if(!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString())){
                        SeckillSkuRedisTo skuRedisTo = new SeckillSkuRedisTo();
                        //缓存商品
                        //1 sku基本数据
                        R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                        if(r.getCode()==0){
                            SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            skuRedisTo.setSkuInfo(skuInfo);
                        }
                        //2 sku秒杀信息
                        BeanUtils.copyProperties(seckillSkuVo,skuRedisTo);
                        //3 设置上当前商品的秒杀时间信息
                        skuRedisTo.setStartTime(session.getStartTime().getTime());
                        skuRedisTo.setEndTime(session.getEndTime().getTime());


                        skuRedisTo.setRandomCode(token);

                        String s = JSON.toJSONString(skuRedisTo);
                        ops.put(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString(),s);
                        //如果当前这个场次商品的库存信息已经上架就不需要上架
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        //可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
                    }
                });
            });

        }

    }
}
