package com.ai.service.impl;

import com.ai.dto.UserBrowseHistoryDTO;
import com.ai.dto.UserDTO;
import com.ai.dto.UserFavoriteDTO;
import com.ai.exception.BusinessException;
import com.ai.mapper.*;
import com.ai.pojo.Document;
import com.ai.pojo.LogInfo;
import com.ai.pojo.PageResult;
import com.ai.service.UserService;
import com.ai.utils.CurrentHolder;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.util.StringUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.print.Doc;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private LogMapper logMapper;
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    @Autowired
    private UserBrowseHistoryMapper userBrowseHistoryMapper;

    @Override
    public UserDTO getUserById(Long userId) {
        return userMapper.getUserById(userId);
    }

    @Override
    public UserDTO updateProfile(UserDTO userDTO) {
        // 1. 调用修改资料专用方法（动态 SQL）
        userMapper.updateUserProfile(userDTO);

        // 2. 查询并返回最新的用户信息
        return userMapper.getUserById(userDTO.getId());
    }

    @Override
    public void updataPassword(UserDTO userDTO) {
        // 1. 获取当前登录用户 ID（绝对不依赖前端传的 name）
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        // 2. 用 ID 查询用户（之前的 getUserById 肯定存在）
        UserDTO currentUser = userMapper.getUserById(userId);

        // 3. 显式处理空值（防止数据库里突然没了这个用户）
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 4. 校验旧密码（确保 currentUser 非空后再调用）
        if (!passwordEncoder.matches(userDTO.getOldPassword(), currentUser.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 5. 加密新密码并更新
        String encodedNewPassword = passwordEncoder.encode(userDTO.getNewPassword());
        userMapper.updatePassword(userId, encodedNewPassword);
    }

    @Override
    public PageResult<LogInfo> getUserLogById(Integer page, Integer size) {
        // 获取当前登录用户 ID
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        // 设置分页参数(PageHelper)
        PageHelper.startPage(page, size);

        // 2. 执行查询
        List<LogInfo> logList = logMapper.list(userId);

        // 3. 强转 Page 对象获取分页信息
        Page<LogInfo> logPage = (Page<LogInfo>) logList;

        // 4. 封装返回（完全匹配接口文档）
        return new PageResult<>(
                logPage.getTotal(),    // 总条数
                logPage.getPageNum(),  // 当前页
                logPage.getPageSize(), // 每页条数
                logPage.getResult()    // 数据列表
        );
    }

    @Override
    public PageResult<UserFavoriteDTO> getFavoritesList(Integer page, Integer size) {
        Long userId = CurrentHolder.getCurrentId();

        // 1. 【关键】开启分页（必须在查询 SQL 之前调用）
        PageHelper.startPage(page, size);

        // 2. 执行查询（PageHelper 会自动拦截 SQL，添加 LIMIT 分页）
        Page<UserFavoriteDTO> pageData = userFavoriteMapper.selectFavoritesByUserId(userId);

        // 3. 封装成统一的 PageResult 返回
        return new PageResult<>(
                pageData.getTotal(),    // 总条数
                pageData.getPageNum(),  // 当前页
                pageData.getPageSize(), // 每页条数
                pageData.getResult()    // 数据列表
        );
    }

    @Override
    public PageResult<UserBrowseHistoryDTO> getBrowseHistory(Integer page, Integer size) {
        Long userId = CurrentHolder.getCurrentId();

        // 1. 【关键】开启分页（必须在查询 SQL 之前调用）
        PageHelper.startPage(page, size);

        // 2. 执行查询（PageHelper 会自动拦截 SQL，添加 LIMIT 分页）
        Page<UserBrowseHistoryDTO> pageData = userBrowseHistoryMapper.selectBrowseHistoriesByUserId(userId);

        // 3. 封装成统一的 PageResult 返回
        return new PageResult<>(
                pageData.getTotal(),    // 总条数
                pageData.getPageNum(),  // 当前页
                pageData.getPageSize(), // 每页条数
                pageData.getResult()    // 数据列表
        );
    }

    @Override
    public List<UserDTO> searchUsers(String keyword) {
        List<UserDTO> users = userMapper.searchUsers(keyword);
        return users.stream()
                .map(u -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(u.getId());
                    dto.setName(u.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }


}