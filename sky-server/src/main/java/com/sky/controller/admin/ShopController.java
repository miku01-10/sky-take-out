package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
//管理端店铺相关
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "管理员店铺管理")
@Slf4j
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    public static final String KEY = "SHOP_STATUS";
    // 设置店铺的营业状态

    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status) {
        log.info("修改店铺状态为：{}", status);
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }
    //查询店铺状态
    @GetMapping("/status")
    public Result getStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("查询店铺状态：{}", shopStatus);
        return Result.success(shopStatus);
    }

}
