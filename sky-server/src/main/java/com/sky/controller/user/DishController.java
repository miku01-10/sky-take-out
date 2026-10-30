package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
     @Autowired
     private RedisTemplate redisTemplate;
    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        //创建redis字段，把菜品列表缓存到redis中
        String Key = "dish_" + categoryId;
        //查询redis中是否有缓存数据
        List<DishVO> List = (List<DishVO>) redisTemplate.opsForValue().get(Key);
        //如果有不用看数据库
        if (List != null&&List.size()>0) {
            return Result.success(List);
        }





        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        //如果没有查询数据库把查询的结果返回到redis中
        List = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(Key, List);//把查询结果存入redis

        return Result.success(List);
    }

}
