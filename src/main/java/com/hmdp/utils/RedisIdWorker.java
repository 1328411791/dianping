package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;

@Component
public class RedisIdWorker {

    private static final long BEGIN_STAMP = 1640995200L;

    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix) {
        // 生成时间戳
        LocalDateTime now=LocalDateTime.now();
        long nowSecond=now.toEpochSecond(ZoneOffset.UTC);
        long timestamp=nowSecond-BEGIN_STAMP;

        String data = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key="icr:"+keyPrefix+":"+data+":";
        long count= stringRedisTemplate.opsForValue().increment(key);
        return (timestamp << COUNT_BITS) | count;
    }


}
