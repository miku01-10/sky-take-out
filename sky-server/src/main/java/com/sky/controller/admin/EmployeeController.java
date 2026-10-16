package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "员工登录", notes = "员工登录，返回jwt令牌")//swagger注解，用于生成接口文档时显示的注释
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();//载荷
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),//密钥
                jwtProperties.getAdminTtl(),//过期时间
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation(value = "员工退出", notes = "员工退出，清除jwt令牌")//swagger注解，用于生成接口文档时显示的注释
    public Result<String> logout() {
        return Result.success();
    }
//新增员工
    @PostMapping
    @ApiOperation(value = "新增员工", notes = "新增员工，返回员工id")//swagger注解，用于生成接口文档时显示的注释
    //使用dto是为了方便前端调用，避免直接操作实体类
    //在新增员工时，需要将dto转换为实体类
    public Result save(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }
    //分页查询
    @GetMapping("/page")
    @ApiOperation(value = "分页查询", notes = "分页查询，返回员工列表")//swagger注解，用于生成接口文档时显示的注释
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("分页查询：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);//赋值给pageResult对象，表示分页查询的结果
        return Result.success(pageResult);
    }
    //修改员工状态
    //启用禁用员工
    @PostMapping("/status/{status}")
    @ApiOperation(value = "启用禁用员工", notes = "启用禁用员工")//swagger注解，用于生成接口文档时显示的注释
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("启用禁用员工：{}", id);
        employeeService.startOrStop(status, id);
        return Result.success();
    }
    //根据id查询员工回显
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询员工", notes = "根据id查询员工，返回员工对象")//swagger注解，用于生成接口文档时显示的注释
    public Result<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工：{}", id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }
    //修改员工
    @PutMapping
    @ApiOperation(value = "修改员工", notes = "修改员工，返回员工对象")//swagger注解，用于生成接口文档时显示的注释
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        log.info("修改员工：{}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }
}
