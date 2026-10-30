package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void addShopingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断购物车中是否已经存在该菜品（根据套餐id跟用户id）
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);//拷贝属性,把shoppingCartDTO中的属性值赋值给shoppingCart
        Long userId = BaseContext.getCurrentId();//通过线程获取当前用户id
        shoppingCart.setUserId(userId);//设置用户id
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);//查询购物车中是否已经存在该菜品

        //如果存在，则数量加一
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);//获取购物车中已存在的该菜品
            cart.setNumber(cart.getNumber() + 1);//数量加一
            shoppingCartMapper.updateNumberById(cart);
        } else {       //如果不存在，则添加到购物车
            //首先判断是什么类型，菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            //如果dishId不为空，则是菜品
             if(dishId != null){
               Dish dish = dishMapper.getById(dishId);//查询菜品
               shoppingCart.setName(dish.getName());//设置菜品名称
               shoppingCart.setImage(dish.getImage());//设置菜品图片
               shoppingCart.setAmount(dish.getPrice());//设置菜品价格


             }else {
                 //如果dishId为空，则是套餐
                 Long setmealId = shoppingCartDTO.getSetmealId();
                 Setmeal setmeal = setmealMapper.getById(setmealId);//查询套餐
                 shoppingCart.setName(setmeal.getName());//设置套餐名称
                 shoppingCart.setImage(setmeal.getImage());//设置套餐图片
                 shoppingCart.setAmount(setmeal.getPrice());//设置套餐价格


             }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
             shoppingCartMapper.insert(shoppingCart);
        }


    }
    //显示购物车
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();//构建购物车对象
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }
    //
    @Override
    public void cleanShoppingCart() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.cleanShoppingCart(userId);
    }
}
