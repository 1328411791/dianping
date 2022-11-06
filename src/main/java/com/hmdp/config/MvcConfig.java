package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.ReferTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new  ReferTokenInterceptor(stringRedisTemplate))
                .excludePathPatterns("/**").order(0);

        registry.addInterceptor(new  LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop-type/**",
                        "/voucher/**",
                        "/shop/**",
                        "/blog/hot").order(1);

    }
}
