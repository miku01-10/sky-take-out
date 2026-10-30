package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    //营业额统计
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //创建集合用来存储开始到结束的日期
        List<LocalDate> dateList = new ArrayList<>();
        //用来储存订单金额
        List<Double> turnoverList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);//日期加一天
            dateList.add(begin);
        }
        for (LocalDate date : dateList){
            //根据日期查询营业额,营业额指的是已完成状态的订单金额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//结束时间
            Map<String, Object> map = new HashMap<>();//创建一个Map集合用来存储查询条件
            map.put("begin", beginTime);//开始时间
            map.put("end", endTime);//结束时间
            map.put("status", Orders.COMPLETED);//订单状态完成
            Double turnover = orderMapper.sumByMap(map);//根据查询条件查询营业额
            turnover = turnover == null ? 0.0 : turnover;//营业额为null则设为0.0
            turnoverList.add(turnover);

        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();

    }

    //用户统计
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放从开始到结束的日期
        List<LocalDate> dateList = new ArrayList<>();
        //存放新增用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放总用户数量
        List<Integer> totalUserList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);//日期加一天
            dateList.add(begin);
        }
        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//结束时间
            Map<String, Object> map = new HashMap<>();//创建一个Map集合用来存储查询条件

            map.put("end", endTime);//结束时间
            Integer totalUserCount = userMapper.countByMap(map);//查询总用户数量
            totalUserList.add(totalUserCount);

            map.put("begin", beginTime);//开始时间
            Integer newUserCount = userMapper.countByMap(map);//根据查询条件查询新增用户数量
            newUserList.add(newUserCount);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }
    //订单统计
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        //存放从开始到结束的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);//日期加一天
            dateList.add(begin);
        }
        //遍历集合查询每天的订单数跟有效订单数
        List<Integer> orderCountList = new ArrayList<>();//存放每天的订单数
        List<Integer> validOrderCountList = new ArrayList<>();//存放每天的有效订单数
        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//结束时间
            Map<String, Object> map = new HashMap<>();//创建一个Map集合用来存储查询条件
            map.put("begin", beginTime);//开始时间
            map.put("end", endTime);//结束时间
            Integer totalOrderCount = orderMapper.countByMap(map);//查询总订单数
            orderCountList.add(totalOrderCount);
            map.put("status", Orders.COMPLETED);//有效订单：已完成状态
            Integer validOrderCount = orderMapper.countByMap(map);//查询有效订单数
            validOrderCountList.add(validOrderCount);
        }
        //计算总订单数和有效订单数
        int totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();//计算总订单数
        int validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();//计算有效订单数
        //计算订单完成率
        double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = (double) validOrderCount / totalOrderCount * 100;//订单完成率
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))//日期
                .orderCountList(StringUtils.join(orderCountList, ","))//每天的订单数
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))//每天的有效订单数
                .totalOrderCount(totalOrderCount)//总订单数
                .validOrderCount(validOrderCount)//有效订单数
                .orderCompletionRate(orderCompletionRate)//订单完成率
                .build();
    }
    //销量排名
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);//查询销量排名,获取前十的名字和对应的销量
        nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());//获取前十的名字
        numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());//获取前十的销量

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }
}
