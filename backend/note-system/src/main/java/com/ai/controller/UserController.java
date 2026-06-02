package com.ai.controller;

import com.ai.annotation.OpLog;
import com.ai.dto.UserBrowseHistoryDTO;
import com.ai.dto.UserDTO;
import com.ai.dto.UserFavoriteDTO;
import com.ai.dto.UserStatisticsDTO;
import com.ai.pojo.LogInfo;
import com.ai.pojo.PageResult;
import com.ai.pojo.Result;
import com.ai.service.UserService;
import com.ai.service.UserStatisticsService;
import com.ai.utils.CurrentHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/user")

@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserStatisticsService userStatisticsService;

    /*
        根据id查询用户信息
     */
    @GetMapping("/profile")
    public Result<UserDTO> getProfile(){
        log.info("获取用户信息");
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        UserDTO userDTO = userService.getUserById(userId);
        if (userDTO == null) {
            return Result.error("用户不存在");
        }
        userDTO.setPassword(null);
        return Result.success(userDTO);
    }

    /**
     * 修改个人资料
     */
    @PutMapping("/profile")
    @OpLog(operation = "CHANGE_PROFILE", targetType = "USER", targetId = "#currentUserId")
    public Result<UserDTO> updateProfile(@RequestBody UserDTO userDTO){
        log.info("修改用户信息: {}", userDTO);
        // 1. 从 ThreadLocal 获取当前登录用户 ID（确保只能修改自己的资料）
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 2. 强制设置用户 ID（防止前端传入其他用户的 ID 进行越权修改）
        userDTO.setId(userId);

        // 3. 过滤敏感字段（绝对不允许通过此接口修改的字段）
        userDTO.setPassword(null);        // 密码需通过单独的修改密码接口
        userDTO.setRole(null);            // 角色由管理员控制
        userDTO.setStatus(null);          // 账号状态由管理员控制
        userDTO.setCreatedAt(null);       // 创建时间不可修改
        userDTO.setLastLoginTime(null);   // 最后登录时间由登录接口更新

        // 4. 调用 Service 层更新并获取最新用户信息
        UserDTO updatedUser = userService.updateProfile(userDTO);

        // 5. 返回更新后的完整数据（前端可直接刷新页面）
        return Result.success(updatedUser);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @OpLog(operation = "CHANGE_PASSWORD", targetType = "USER", targetId = "#currentUserId")
    public Result<String> updatePassword(@RequestBody UserDTO userDTO){
        log.info("修改密码: {}", userDTO);
        userService.updataPassword(userDTO);
        return Result.success();
    }

    /**
     * 获取操作日志
     */
    @GetMapping("/logs")
    public Result<PageResult<LogInfo>> getLogs(
            // 前端不传则默认第1页
            @RequestParam(defaultValue = "1") Integer page,
            // 前端不传则默认20条/页
            @RequestParam(defaultValue = "20") Integer size
    ){
        log.info("获取用户日志");
        PageResult<LogInfo> pageResult = userService.getUserLogById(page, size);
        return Result.success(pageResult);
    }




    @GetMapping("/favorites")
    public Result<PageResult<UserFavoriteDTO>> getFavoritesList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ){
        log.info("获取收藏列表");
        PageResult<UserFavoriteDTO> pageResult = userService.getFavoritesList(page, size);
        return Result.success(pageResult);
    }

    @GetMapping("/browse-history")
    public Result<PageResult<UserBrowseHistoryDTO>> getBrowseHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ){
        log.info("获取浏览历史");
        PageResult<UserBrowseHistoryDTO> pageResult = userService.getBrowseHistory(page, size);
        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    public Result<UserStatisticsDTO> getStatistics(){
        log.info("获取用户统计信息");
        UserStatisticsDTO userStatistics = userStatisticsService.getStatistics();
        return Result.success(userStatistics);
    }


    @GetMapping("/search")
    public Result<List<UserDTO>> searchUsers(@RequestParam String keyword) {
        List<UserDTO> list = userService.searchUsers(keyword);
        return Result.success(list);
    }

    @GetMapping("/{userId}")
    public Result<UserDTO> getUserById(@PathVariable Long userId) {
        log.info("获取用户信息：{}", userId);
        UserDTO userDTO = userService.getUserById(userId);
        if (userDTO == null) {
            return Result.error("用户不存在");
        }
        userDTO.setPassword(null);
        return Result.success(userDTO);
    }
}