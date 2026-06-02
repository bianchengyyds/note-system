package com.ai.controller;

import com.ai.annotation.OpLog;
import com.ai.common.Views;
import com.ai.dto.LoginResponseDTO;
import com.ai.dto.UserDTO;
import com.ai.pojo.Result;
import com.ai.service.AuthService;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /*
        用户注册
     */
    // 注册接口：使用基础视图
    @PostMapping("/register")
    public Result<UserDTO> register(@RequestBody UserDTO userDTO){
        log.info("用户注册: {}", userDTO);
        UserDTO userInfo = authService.register(userDTO);
        return Result.success(userInfo);
    }

    /*
        用户登录
     */
    @PostMapping("/login")
    @OpLog(operation = "USER_LOGIN", targetType = "")
    public Result<LoginResponseDTO> login(@RequestBody UserDTO userDTO){
        log.info("用户登录: {}", userDTO);
        LoginResponseDTO loginResult = authService.login(userDTO);
        // 输出token
        log.info("token: {}", loginResult.getToken());
        return Result.success(loginResult);
    }
}