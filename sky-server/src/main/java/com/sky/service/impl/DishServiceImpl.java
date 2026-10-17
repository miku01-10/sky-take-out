package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    //新增菜品和对应的口味
    @Override
    @Transactional//事务管理，确保数据的一致性
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();//DishDTO包含了菜品的口味这里面只需要菜品的基本信息，所有用一个实体类dish接受就行
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表插入1个数据
        dishMapper.insert(dish);
        //获取菜品的id
        Long dishId = dish.getId();//这个是在DishFlavorMapper.xml中设置的，
        // 因为DishFlavorMapper.xml中设置了useGeneratedKeys="true" keyProperty="id"，所以这里可以获取到

        //向菜品口味表插入n个数据
        List<DishFlavor> flavors = dishDTO.getFlavors();//获取菜品口味
        //判断口味是否为空
        if (flavors != null && flavors.size() > 0){
            for (DishFlavor Dishflavor : flavors) {
                //设置菜品id
                Dishflavor.setDishId(dishId);
            }
            //向菜品口味表插入n个数据
            dishFlavorMapper.insertBatch(flavors);

        }
    }
    //分页查询菜品
    @Override

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品：{}", dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
    @Override
    @Transactional//事务管理，确保数据的一致性
    public void delete(List<Long> ids) {
        //判断菜品是否起售
      for (Long id : ids){
          Dish dish = dishMapper.getById(id);
          if (dish.getStatus() == 1){
              //菜品起售中，不能删除
              throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
          }
      }
        //是否被套餐关联,去setmeal_dish中去查询是否有关联
        List<Long> setmealDishIds = setmealDishMapper.getByDishIds(ids);
      if (setmealDishIds != null && setmealDishIds.size() > 0){
          log.info("菜品{}被套餐{}关联，不能删除", ids, setmealDishIds);
          throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
      }
//        //删除菜品
//        for (Long id : ids){
//            dishMapper.deleteById(id);
//        //删除菜品口味
//
//            dishFlavorMapper.deleteByDishId(id);
//        }
         //根据菜品id集合批量删除菜品
        dishMapper.deleteByIds(ids);
      //删除菜品口味
        dishFlavorMapper.deleteByDishIds(ids);
    }

    //根据id查询菜品和对应的口味
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        log.info("根据id查询菜品和对应的口味：{}", id);
        //根据id查询菜品
        Dish dish = dishMapper.getById(id);
        //根据id查询菜品口味
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        //封装成dishvo
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }
    @Override
    @Transactional//事务管理，确保数据的一致性
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品的基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);//这里不用dto是因为dto中包含口味信息，而这里只需要菜品信息
        //删除原有的菜品口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //插入新的菜品口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
            }
            dishFlavorMapper.insertBatch(flavors);
        }

    }
}
