package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryByList() {
        // 从缓存中获取数据
        String shopTypeListJson = stringRedisTemplate.opsForValue().get("cache:shopType:list");

        if(shopTypeListJson!=null){
            return JSONUtil.toList(JSONUtil.parseArray(shopTypeListJson),ShopType.class);
        }

        // 从数据库中查询数据
        List<ShopType> shopTypelist = list();

        if (shopTypelist==null){
            // 将数据存入缓存
            return null;
        }

        stringRedisTemplate.opsForValue().set("cache:shopType:list",JSONUtil.toJsonStr(shopTypelist));

        return shopTypelist;
    }
}
