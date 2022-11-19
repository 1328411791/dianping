package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }

        String randomNumbers = RandomUtil.randomNumbers(6);

        redisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY
                +phone,randomNumbers,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.debug("ok " + randomNumbers);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        String phone=loginForm.getPhone();

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }
        String cacheCode =redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY
                +phone);
        String code=loginForm.getCode();

        if(session.toString().equals(code)||phone==null){
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone", loginForm.getPhone()).one();

        if(user==null){
            user=creatUserWithPhone(loginForm.getPhone());
        }

        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO=BeanUtil.copyProperties(user,UserDTO.class);

        Map<String,Object> userMap=BeanUtil.beanToMap(userDTO);

        redisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY
                +token,userMap);
        redisTemplate.expire(RedisConstants.LOGIN_CODE_KEY+token,
                RedisConstants.LOGIN_USER_TTL,TimeUnit.SECONDS);

        return Result.ok();
    }

    private User creatUserWithPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(8));
        return user;
    }
}
