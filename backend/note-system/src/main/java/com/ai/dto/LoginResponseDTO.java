package com.ai.dto;

import com.ai.common.Views;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token; // 登录令牌
    private UserDTO userInfo; // 用户核心信息
}