package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Slf4j
@Api("微信登录")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;
    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信登录，code：{}", userLoginDTO.getCode());
        //微信登录
        User user = userService.wxLogin(userLoginDTO);
        //生成jwt令牌
        Map<String, Object> claims = new HashMap<>();//载荷
        claims.put(JwtClaimsConstant.USER_ID, user.getId());//用户id

        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),jwtProperties.getUserTtl(),claims);//生成令牌
        UserLoginVO userLoginVO = UserLoginVO.builder()//构建用户登录VO
                .id(user.getId())//用户id
                .openid(user.getOpenid())
                .token(token)//令牌
                .build();//构建用户登录VO
        return Result.success(userLoginVO);
    }
}
