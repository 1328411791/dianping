package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReferTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public ReferTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("authorization");

        if(StrUtil.isBlank(token)){
            return true;
        }

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash()
                .entries(RedisConstants.LOGIN_USER_KEY + token);

        if(userMap.isEmpty()){
            return true;
        }

        UserDTO userDTO= BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);

        UserHolder.saveUser(userDTO);

        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token
                ,RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);


        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}