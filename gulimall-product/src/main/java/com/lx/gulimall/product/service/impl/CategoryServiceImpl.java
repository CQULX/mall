package com.lx.gulimall.product.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lx.gulimall.product.service.CategoryBrandRelationService;
import com.lx.gulimall.product.vo.Catelog2Vo;
import jakarta.json.Json;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.common.utils.PageUtils;
import com.lx.common.utils.Query;

import com.lx.gulimall.product.dao.CategoryDao;
import com.lx.gulimall.product.entity.CategoryEntity;
import com.lx.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    private Map<String, Object> cache = new HashMap<>();

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {

        List<CategoryEntity> entities = baseMapper.selectList(null);

        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                Objects.equals(categoryEntity.getParentCid().intValue(), 0)
        ).map(menu -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> list) {

        baseMapper.deleteBatchIds(list);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }


//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
//            @CacheEvict(value = "category",key = "'getCatalogJson'")
//    })
    @CacheEvict(value = "category",allEntries=true)
    @Transactional
    //CachePut() 双写模式
    @Override
    public void updateDetail(CategoryEntity category) {
        this.updateById(category);
        if (!StringUtils.isEmpty(category.getName())) {
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
        }
    }

    //每个需要缓存的数据，我们指定将其放入哪个名字的缓存【缓存的分区】（按业务类型分）
    @Cacheable(value = {"category"},key = "#root.method.name") //代表当前方法的结果需要缓存
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }


    @Cacheable(value = {"category"},key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库");

        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        //查出所有一级分类
        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);


        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查到所有一级分类的二级分类
            List<CategoryEntity> categoryLevel2Entity = getParentCid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryLevel2Entity != null) {
                catelog2Vos = categoryLevel2Entity.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //查询三级分类
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        //封装指定格式数据
                        List<Catelog2Vo.Catelog3Vo> catelog3VoList = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3VoList);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());

            }

            return catelog2Vos;
        }));

        return parent_cid;

    }

//    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        //缓存中存取的数据均为Json字符串
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存不命中，查询数据库...");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();
            //将查询出来的对象转换为Json放入缓存中

            return catalogJsonFromDb;
        }
        System.out.println("缓存命中...直接返回...");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }


        return dataFromDb;


    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
//加锁和设置过期时间得是原子操作
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功...");
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                //lua脚本
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                //原子删锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);

            }
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if(uuid.equals(lockValue)){
//                //删除自己的锁
//                redisTemplate.delete("lock"); //删除锁
//            }


            return dataFromDb;
        } else {
            System.out.println("获取分布式锁不成功，等待重试...");
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            return getCatalogJsonFromDbWithRedisLock();
        }

    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        System.out.println("查询了数据库");

        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        //查出所有一级分类
        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);


        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查到所有一级分类的二级分类
            List<CategoryEntity> categoryLevel2Entity = getParentCid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryLevel2Entity != null) {
                catelog2Vos = categoryLevel2Entity.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //查询三级分类
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        //封装指定格式数据
                        List<Catelog2Vo.Catelog3Vo> catelog3VoList = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3VoList);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());

            }

            return catelog2Vos;
        }));
        String jsonString = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJson", jsonString, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    //从数据库查询并封装分类数据
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

//
//        Map<String, List<Catelog2Vo>> catalogJson = (Map<String, List<Catelog2Vo>>) cache.get("catalogJson");
//
//        if(cache.get("catalogJson")==null) {
//
//            return catalogJson;
//        }
        synchronized (this) {

            String catalogJson = redisTemplate.opsForValue().get("catalogJson");
            if (!StringUtils.isEmpty(catalogJson)) {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
                return result;
            }
            System.out.println("查询了数据库");

            List<CategoryEntity> selectList = this.baseMapper.selectList(null);
            //查出所有一级分类
            List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);


            //封装数据
            Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //查到所有一级分类的二级分类
                List<CategoryEntity> categoryLevel2Entity = getParentCid(selectList, v.getCatId());
                List<Catelog2Vo> catelog2Vos = null;
                if (categoryLevel2Entity != null) {
                    catelog2Vos = categoryLevel2Entity.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //查询三级分类
                        List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                        if (level3Catelog != null) {
                            //封装指定格式数据
                            List<Catelog2Vo.Catelog3Vo> catelog3VoList = level3Catelog.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                                return catelog3Vo;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(catelog3VoList);
                        }

                        return catelog2Vo;
                    }).collect(Collectors.toList());

                }

                return catelog2Vos;
            }));
            String jsonString = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJson", jsonString, 1, TimeUnit.DAYS);
            return parent_cid;
        }


    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
        //return this.baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }


    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return Objects.equals(categoryEntity.getParentCid(), root.getCatId());
        }).map(menu -> {
            menu.setChildren(getChildrens(menu, all));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return children;
    }
}