package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
    private final String LOCK_PREFIX = "lock:";
    private final String ID= UUID.randomUUID().toString(true)+"-";
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = ID+ Thread.currentThread().getId();
        String key=LOCK_PREFIX+this.name;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, threadId, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success);
    }

    @Override
    public void unlock() {
        String key=LOCK_PREFIX+this.name;
        String treadId=ID+Thread.currentThread().getId();
        String RedisTreadId = stringRedisTemplate.opsForValue().get(key);
        if (Objects.equals(RedisTreadId, treadId)){
            stringRedisTemplate.delete(key);
        }
    }
}
