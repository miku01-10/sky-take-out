package com.sky.controller.admin;

import com.sky.entity.User;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "数据统计")
public class ReportController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    //营业额统计
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("开始执行营业额统计");
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }
    //用户统计
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("开始执行用户统计");
        return Result.success(reportService.getUserStatistics(begin, end));
    }
    //订单统计
    @GetMapping("/orderStatistics")
    public Result<OrderReportVO> orderStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("开始执行订单统计");
        return Result.success(reportService.getOrderStatistics(begin, end));
    }
    //查询销量排名
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> salesTop10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("开始执行查询销量排名");
        return Result.success(reportService.getSalesTop10(begin, end));
    }

    //导出运营数据
    @GetMapping("/export")
    public void  export(HttpServletResponse response)
    {
        log.info("开始执行导出运营数据");
        reportService.exportBusinessData(response);
    }
}
