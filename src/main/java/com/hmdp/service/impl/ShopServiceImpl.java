package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
        //
        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class
                , this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
    }

    public Shop queryWithMutex(Long id) {
        // 缓存穿透
        // 1.查询商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);

        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中
        if(shopJson!=null){
            return null;
        }
        Shop shop;
        String lockKey = "lock:shop:" + id;
        try {

            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            shop = getById(id);
            if (shop == null) {
                // 空数据写入缓存
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,
                        "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);

                return null;
            }

            stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop)
                    , RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            unlock(lockKey);
        }
        return shop;
    }



    private boolean tryLock(String key){
      Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",RedisConstants.LOCK_SHOP_TTL,TimeUnit.SECONDS);
      return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {

        Long id=shop.getId();

        if(id==null){
            return Result.fail("商铺不存在");
        }

        updateById(shop);

        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+shop.getId());

        return Result.ok();
    }
}
