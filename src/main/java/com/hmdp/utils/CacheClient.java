package com.hmdp.utils;


import cn.hutool.core.lang.func.Func;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate redisTemplate){
        this.stringRedisTemplate=redisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit){
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    public <R,ID>R queryWithPassThrough(String keyPrefix, ID id, Class<R> type
            , Function<ID,R> dbFallBack,Long time,TimeUnit timeUnit){
       String key=keyPrefix+id;

        // 缓存穿透
        String json = stringRedisTemplate.opsForValue().get(key);

        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }
        // 判断命中是不是空
        if(json!=null){
            return null;
        }

        R r =dbFallBack.apply(id);
        if(r==null){
            // 空数据写入缓存
            stringRedisTemplate.opsForValue().set(key,
                    "",time, timeUnit);

            return null;
        }

        this.set(key,r,time,timeUnit);

        return r;
    }

    public <R,ID> R queryWithLogicalExprie(String keyPrefix,ID id,Class<R> type
            ,Function<ID,R> dbFallBack,Long time,TimeUnit timeUnit) {
        // 缓存穿透
        String key=keyPrefix+id;
        // 1.查询商铺信息
        String shopJson = stringRedisTemplate.opsForValue()
                .get(key);

        if(StrUtil.isBlank(shopJson)){
            return null;
        }

        RedisData redisData=JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data=(JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }

        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    R r1 = dbFallBack.apply(id);
                    this.setWithLogicalExpire(key,r1,time,timeUnit);
                }catch (Exception e){
                    throw  new RuntimeException(e);
                }
                finally {
                    unlock(lockKey);
                }
            });
        }
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",RedisConstants.LOCK_SHOP_TTL,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
