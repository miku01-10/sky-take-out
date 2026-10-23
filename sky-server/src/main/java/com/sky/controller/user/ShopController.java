package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "商户店铺相关")
@Slf4j

public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    public static final String KEY = "SHOP_STATUS";
    //商户查看店铺状态
 @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("商户查看店铺状态：{}", shopStatus);
        return Result.success(shopStatus);
    }
}
