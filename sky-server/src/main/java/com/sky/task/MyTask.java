//package com.sky.task;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.scheduling.annotation.Schedules;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
////定时任务
//@Component//声明这是一个定时任务类
//@Slf4j
//public class MyTask {
//
//    @Scheduled(cron = "0/5 * * * * ?")//声明这是一个定时任务，cron表达式表示每5秒执行一次
//    public void doTask() {
//        log.info("定时任务执行了...",new Date());
//    }
//}
