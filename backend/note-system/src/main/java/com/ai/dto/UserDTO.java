package com.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 仅序列化非null字段
public class UserDTO {
    // --- 注册/登录/详情通用返回字段 ---
    private Long id;
    private String name;
    private String role;

    // --- 仅登录返回的token（只读，不接收前端入参） ---
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String token;

    // --- 仅详情接口返回的扩展字段 ---
    private String email;
    private String phone;
    private Integer gender;
    private String avatar;
    private String bio;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- 仅接收前端入参、绝对不返回的敏感字段 ---
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String oldPassword;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String newPassword;

    private Long noteCount;
    private Long kbCount;
}