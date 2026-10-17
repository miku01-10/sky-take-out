package com.sky.aspect;
//定义切面类。
import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect//表示这是一个切面类
@Component//表示这是一个Spring组件
@Slf4j//表示这是一个日志记录器
public class AutoFillAspect {
    //定义切点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")//表示匹配com.sky.mapper包下的所有方法，并且这些方法上带有@AutoFill注解
    //这个方法是切点，用于指定切入点表达式
    public void autoFillPointCut() {
        log.info("切点被调用");
    }

    @Before("autoFillPointCut()")
    //这个方法是切面，用于在方法执行前进行拦截
    public void autoFillBefore(JoinPoint joinPoint) {
        log.info("切面被调用");
        //获取当前被拦截的方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//获取方法签名
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获取方法上的AutoFill注解
        OperationType operationType = autoFill.value();//获取注解上的操作类型

        //获取到当前拦截方法的参数，实体对象
        Object[] args = joinPoint.getArgs();//获取方法参数
        if (args.length == 0) {
            return;//如果没有参数，直接返回
        }

        Object entity = args[0];//将参数转换为实体对象,这里假设第一个参数是实体对象

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();//获取当前时间
        Long currentId = BaseContext.getCurrentId();//获取当前用户ID
        //根据当前不同的操作类型，给实体对象的属性赋值（反射）
        if (operationType == OperationType.INSERT){
            //如果是插入操作，需要设置创建人和更新人
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                //具体解释：获取实体对象的setCreateTime方法，参数类型为LocalDateTime
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                //获取实体对象的setUpdateTime方法，参数类型为LocalDateTime
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                //获取实体对象的setUpdateUser方法，参数类型为Long
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setCreateTime.invoke(entity, now);//调用实体对象的setCreateTime方法，参数为当前时间
                //具体解释：调用实体对象的setCreateTime方法，参数为当前时间
                setCreateUser.invoke(entity, currentId);//等价于entity.setCreateUser(currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                log.error("反射调用方法失败", e);
            }
        }
        else if (operationType == OperationType.UPDATE){
            //如果是更新操作，需要设置更新人
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                log.error("反射调用方法失败", e);
            }
        }

    }


}
