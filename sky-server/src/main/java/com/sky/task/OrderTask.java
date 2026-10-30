package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    //定时处理超时订单
    @Scheduled(cron = "0 * * * * ?")//每分钟执行一次
    public void processTimeOrder() {
        log.info("定时处理超时订单...", new Date());
        //查询超时订单。判断订单状态，待付款并且超时时间大于15分钟的取消订单（当前时间-15）
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(15));
        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);//取消订单
                orders.setCancelTime(LocalDateTime.now());//设置取消时间
                orders.setCancelReason("订单超时取消");//设置取消原因
                orderMapper.update(orders);//更新订单
            }
        }
        log.info("取消订单完成");
    }

    //定时处理待取货订单，一直处于待取货状态的订单
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨1点执行
    public void processDeliveryOrder() {
        log.info("定时处理待取货订单...", new Date());
        //查询待取货订单。判断订单状态，待取货并且订单创建时间是上一天的完成订单（当前时间-1小时）
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().minusMinutes(60));
        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);//完成订单
                orders.setCancelTime(LocalDateTime.now());//设置完成时间
                orderMapper.update(orders);//更新订单
            }
        }
        log.info("完成订单完成");
    }
}
