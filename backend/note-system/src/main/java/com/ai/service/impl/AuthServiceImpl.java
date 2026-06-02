package com.ai.service.impl;

import com.ai.dto.LoginResponseDTO;
import com.ai.dto.UserDTO;
import com.ai.exception.BusinessException;
import com.ai.mapper.UserMapper;
import com.ai.service.AuthService;
import com.ai.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /*
        用户注册
     */
    @Transactional
    @Override
    public UserDTO register(UserDTO userDTO) {
        // 1. 参数校验
        if (userDTO.getName() == null || userDTO.getName().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }

        // 2. ✅ 数据清洗：将空字符串转为 null
        if (userDTO.getEmail() != null && userDTO.getEmail().isEmpty()) {
            userDTO.setEmail(null);
        }
        if (userDTO.getPhone() != null && userDTO.getPhone().isEmpty()) {
            userDTO.setPhone(null);
        }

        // 3. 检查用户名是否已存在
        UserDTO existingUser = userMapper.selectUserByName(userDTO.getName());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 4. 加密密码
        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());

        // 5. 构建用户对象
        UserDTO user = new UserDTO();
        user.setName(userDTO.getName());
        user.setPassword(encodedPassword);
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setGender(userDTO.getGender() != null ? userDTO.getGender() : 0);
        user.setRole("USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 6. 插入数据库
        userMapper.insertUser(user);

        // 7. 返回结果
        UserDTO result = new UserDTO();
        result.setId(user.getId());
        result.setName(user.getName());
        result.setRole(user.getRole());

        return result;
    }


    @Transactional
    @Override
    public LoginResponseDTO login(UserDTO userDTO) {
        // 1. 原有校验逻辑（用户名、密码、账号状态）保持不变
        UserDTO dbUser = userMapper.selectUserByName(userDTO.getName());
        if (dbUser == null) {
            throw new BusinessException("用户名错误");
        }
        if (!passwordEncoder.matches(userDTO.getPassword(), dbUser.getPassword())) {
            throw new BusinessException("密码错误");
        }
        if (dbUser.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 2. 生成JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", dbUser.getId());
        claims.put("name", dbUser.getName());
        claims.put("role", dbUser.getRole());
        String token = JwtUtils.generateToken(claims);

        // 3. 更新登录时间
        dbUser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateLoginTime(dbUser);

        // 4. 构造返回结构（匹配文档）
        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setToken(token);

        // 构造userInfo核心字段
        UserDTO userInfo = new UserDTO();
        userInfo.setId(dbUser.getId());
        userInfo.setName(dbUser.getName());
        userInfo.setRole(dbUser.getRole());
        responseDTO.setUserInfo(userInfo);

        return responseDTO;
    }
}