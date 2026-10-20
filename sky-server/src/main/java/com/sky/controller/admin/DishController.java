package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//菜品管理
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品管理接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    //新增菜品
    @PostMapping()
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
   }
   //分页查询菜品
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品：{}", dishPageQueryDTO);

        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    //批量删除菜品
    @DeleteMapping
    @ApiOperation(value = "批量删除菜品", tags = "批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品：{}", ids);
        dishService.delete(ids);
        return Result.success();
    }
    //根据id查询菜品(包含口味)
    //vo是view object的缩写，是用于展示的类，通常用于将多个对象封装到一个对象中返回给前端
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询菜品(包含口味)", tags = "根据id查询菜品(包含口味)")
    public Result<DishVO> getById(@PathVariable Long id){
        DishVO dishVO = dishService.getByIdWithFlavor(id);

        return Result.success(dishVO);
    }
    //修改菜品
    @PutMapping
    @ApiOperation(value = "修改菜品", tags = "修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }
    //根据id查询菜品，主要是用来套餐中加菜品的
    @GetMapping("/list")
    @ApiOperation(value = "根据id查询菜品，主要是用来套餐中加菜品的", tags = "根据id查询菜品，主要是用来套餐中加菜品的")
    public Result<List<DishVO>> list(@RequestParam Long categoryId){
        List<DishVO> dishVOList = dishService.list(categoryId);
        return Result.success(dishVOList);
    }
    //修改菜品状态
    @PostMapping("/status/{status}")
    @ApiOperation(value = "修改菜品状态", tags = "修改菜品状态")
    public Result updateStatus(@PathVariable Integer status, @RequestParam Long id){
        log.info("修改菜品状态：{}", status);
        dishService.updateStatus(status, id);
        return Result.success();
    }
}
