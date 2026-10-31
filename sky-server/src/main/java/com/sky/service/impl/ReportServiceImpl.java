package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;
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
//导出业务数据
    @Override
    public void exportBusinessData(HttpServletResponse response) throws IOException {
        //1.查询数据库，获取营业数据--30天内的
        LocalDate begin = LocalDate.now().minusDays(30);//开始时间
        LocalDate end = LocalDate.now().minusDays(1);//结束时间
        //获取营业数据
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        //2.通过POI将数据写入到Excel文件中
        //通过输入流获取模板文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板");
        XSSFWorkbook workbook = new XSSFWorkbook(in);//创建工作簿
        //填充数据



        //获取sheet
        XSSFSheet sheet = workbook.getSheet("Sheet");
        //填充数据--时间
        sheet.getRow(1).getCell(1).setCellValue("时间："+begin + "至" + end);
        //获得第4行
        XSSFRow row = sheet.getRow(3);
        row.getCell(2).setCellValue(businessData.getTurnover());
        row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
        row.getCell(6).setCellValue(businessData.getNewUsers());
        //获取第5行
        row = sheet.getRow(4);
        row.getCell(2).setCellValue(businessData.getValidOrderCount());

        row.getCell(4).setCellValue(businessData.getUnitPrice());

        //填充详细数据
        for (int i = 0; i < 30; i++){
            LocalDate date = begin.plusDays(i);
            BusinessDataVO businessData1 = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
            //填充详细数据,借助i，第一次遍历的时候是第8行，也就是i=0，i+7=7，就是第8行
            row = sheet.getRow(i + 7);
            row.getCell(1).setCellValue(date.toString());
            row.getCell(2).setCellValue(businessData1.getTurnover());//营业额
            row.getCell(3).setCellValue(businessData1.getValidOrderCount());//有效订单数
            row.getCell(4).setCellValue(businessData1.getOrderCompletionRate());//订单完成率
            row.getCell(5).setCellValue(businessData1.getUnitPrice());//单价
            row.getCell(6).setCellValue(businessData1.getNewUsers());//新增用户数

        }


        //3.通过输出流将excel文件下载 到客户端
        ServletOutputStream out = response.getOutputStream();//获取输出流
        workbook.write(out);//将工作簿写入输出流
        //关闭输出流
        out.close();
        workbook.close();

    }
}
