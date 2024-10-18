package com.lx.gulimall.product.web;

import com.lx.gulimall.product.entity.CategoryEntity;
import com.lx.gulimall.product.service.CategoryService;
import com.lx.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        //查询所有的1级分类
        List<CategoryEntity> categoryEntities=categoryService.getLevel1Categorys();
        model.addAttribute("categorys",categoryEntities);
        return "index.html";
    }

    @ResponseBody
    @GetMapping("/index/json/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        Map<String, List<Catelog2Vo>> map=categoryService.getCatalogJson();
        return map;
    }


    @ResponseBody
    @GetMapping("/hello")
    public String hello(){

        RLock lock = redissonClient.getLock("my-lock");
        lock.lock(10, TimeUnit.SECONDS);//阻塞式等待 自动解锁时间一定要大于业务的执行时间
        try {
            System.out.println("加锁成功，执行业务..."+Thread.currentThread().getId());
            Thread.sleep(30000);
        }catch (Exception e) {}
        finally {
            System.out.println("释放锁..."+Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }

    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.writeLock();
        String s="";
        try {

            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue",s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.readLock();

        String s="";
        try {
            rLock.lock();
            s = redisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return s;
    }

    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5L);
        door.await();//等待闭锁都完成

        return "放假了...";
    }

    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        //park.acquire();
        boolean b = park.tryAcquire();
//
//        Semaphore semaphore = new Semaphore(5);
//        semaphore.release();
//        semaphore.acquire();
        return "ok->"+b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();
//
//        Semaphore semaphore = new Semaphore(5);
//        semaphore.release();
//        semaphore.acquire();
        return "ok";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id){
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();
        return id+"班的人都走了...";
    }


}
