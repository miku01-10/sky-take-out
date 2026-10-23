package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    public static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;//微信属性,主要是获取微信属性
    @Autowired
    private UserMapper userMapper;
    //微信登录
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信登录接口，获取openid
        String openid = getOpenid(userLoginDTO);
        //判断openid是否为空，空的抛出异常
        if (openid == null) {
            throw new RuntimeException("微信登录失败");
        }
        User user = userMapper.getUserByOpenid(openid);//根据openid查询用户
        //判断是否为新用户
        if (user == null) {
            //如果是新的，自动完成注册
            user = user.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回这个用户对象
        return user;
    }
    //获取openid

    private String getOpenid(UserLoginDTO userLoginDTO) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());//微信appid
        map.put("secret", weChatProperties.getSecret());//微信密钥
        map.put("js_code", userLoginDTO.getCode());//微信登录凭证
        map.put("grant_type", "authorization_code");//授权类型
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, map);//调用微信登录接口，获取openid
        JSONObject jsonObject = JSONObject.parseObject(json);//将json字符串转换为json对象
        String openid = jsonObject.getString("openid");//获取openid
        return openid;
    }
}
