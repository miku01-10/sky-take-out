package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
// 员工信息DTO
// @Data 注解会自动生成 getter、setter、toString、equals 和 hashCode 方法
//用来接受员工信息的DTO
@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

}
