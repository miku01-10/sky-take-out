package com.sky.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrdersPageQueryDTO implements Serializable {
    // 当前页
    private int page;
    // 每页显示的条数

    private int pageSize;
    // 订单编号

    private String number;
    // 手机号

    private  String phone;
    // 订单状态

    private Integer status;
    // 订单开始时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;
    // 订单结束时间

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    // 用户id

    private Long userId;

}
