package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {
    //新增套餐
    @Autowired
    private SetmealMapper setmealMapper;//套餐Mapper
    @Autowired
    private SetmealDishMapper setmealDishMapper;//套餐菜品Mapper

    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();//先添加套餐的基本信息
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        //添加套餐和菜品的关联信息
        //首先接受到id
        Long id = setmeal.getId();
        //从前端获取到菜品分类信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //判断菜品分类信息是否为空
        if (setmealDishes != null && setmealDishes.size() > 0){
            //将setmealId赋值
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(id);
            }
            //添加菜品分类信息
            setmealDishMapper.insertSetmealDishes(setmealDishes);
        }

    }
    //分页查询
     @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
         PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
         Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
    @Override
    public void deleteBatch(List<Long> ids) {
        //首先判断套餐是否可以删除
        //看套餐是否起售
        for (Long id : ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == 1){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);

            }
        }
       //删除套餐需要删除套餐里面的关系和套餐
        for (Long id : ids){
            setmealMapper.deleteBatch(id);
            setmealDishMapper.deleteBySetmealId(id);
        }

    }
    //根据id查询套餐回显

//    根据id查询套餐和套餐菜品关系
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        log.info("根据id查询套餐和套餐菜品关系");
        Setmeal setmeal = setmealMapper.getById(id);//查询套餐
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);//查询套餐和菜品关系
        SetmealVO setmealVO = new SetmealVO();//创建vo对象，返回给前端
        BeanUtils.copyProperties(setmeal, setmealVO);//将套餐信息复制到vo对象中
        setmealVO.setSetmealDishes(setmealDishes);//将套餐和菜品关系信息设置到vo对象中
        return setmealVO;
    }
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        log.info("修改套餐");
        setmealMapper.update(setmealDTO);//修改
        //修改套餐和菜品关系
        Long id = setmealDTO.getId();//获取被修改的套餐id
        setmealDishMapper.deleteBySetmealId(id);//删除套餐和菜品关系
        //然后增加菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();//获取前端中的菜品分类信息
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);//设置套餐id
        }
        setmealDishMapper.insertSetmealDishes(setmealDishes);//增加菜品信息
    }
    @Override
    public void updateStatus(Integer status, Long id) {
        log.info("修改套餐状态");
        setmealMapper.updateStatus(status, id);
    }
}
