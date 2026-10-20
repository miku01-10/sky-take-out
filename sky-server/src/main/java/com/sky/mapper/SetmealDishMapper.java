package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
// 套餐菜品Mapper
@Mapper
public interface SetmealDishMapper {

    List<Long> getByDishIds(List<Long> dishIds);
   @AutoFill(value = OperationType.INSERT)
    void insertSetmealDishes(List<SetmealDish> setmealDishes);
    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void deleteBySetmealId(Long setmealId);
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id);
}
