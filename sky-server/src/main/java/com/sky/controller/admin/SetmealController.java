package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐管理")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    //新增套餐
    @PostMapping
    @CacheEvict(value = "setmealCache", key = "#setmealDTO.categoryId")//删除setmealCache缓存
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐：{}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }
    //分页查询
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询套餐，第{}页，每页{}条，名称：{}", setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize(), setmealPageQueryDTO.getName());
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }
    //删除套餐
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)//删除所有setmealCache缓存
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除套餐：{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }
    //根据id查询套餐(查询回显)
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }
    //修改套餐

    @PutMapping
    @CacheEvict(value = "setmealCache", allEntries = true)//删除所有setmealCache缓存
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐：{}", setmealDTO);
        setmealService.update(setmealDTO);
        //修改套餐中菜品

        return Result.success();
    }
    //修改套餐状态
    @PostMapping("/status/{status}")
    @CacheEvict(value = "setmealCache", allEntries = true)//删除所有setmealCache缓存
    public Result updateStatus(@PathVariable Integer status, @RequestParam Long id){
        log.info("修改套餐状态：{}", status, id);
        setmealService.updateStatus(status, id);
        return Result.success();
    }
}
