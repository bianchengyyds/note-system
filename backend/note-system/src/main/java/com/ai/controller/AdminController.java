package com.ai.controller;

import com.ai.annotation.OpLog;
import com.ai.dto.*;
import com.ai.mapper.UserStatRefreshTask;
import com.ai.pojo.PageResult;
import com.ai.pojo.Result;
import com.ai.service.AdminService;
import com.ai.service.DailyStatService;
import com.ai.service.OperationLogService;
import com.ai.service.UserAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private UserStatRefreshTask userStatRefreshTask;
    @Autowired
    private DailyStatService dailyStatService;
    @Autowired
    private UserAdminService adminUserService;
    @Autowired
    private OperationLogService operationLogService;

    /*
        获取数据统计信息
     */
    @GetMapping("/statistics/overview")
    public Result<AdminOverviewDTO> getOverview(){
        log.info("获取数据统计信息");
        AdminOverviewDTO overview = adminService.getOverview();
        return Result.success(overview);
    }

    /**
     * 按年度查看每月趋势
     */
    @GetMapping("/statistics/yearly")
    public Result<List<MonthlyStatDTO>> yearly(
            @RequestParam(required = false) Integer year
    ) {
        log.info("按年度查看每月趋势");
        int y = (year != null) ? year : YearMonth.now().getYear();
        List<MonthlyStatDTO> list = adminService.getYearlyStats(y);
        return Result.success(list);
    }

    /**
     * 查看本月汇总（单对象）
     */
    @GetMapping("/statistics/monthly")
    public Result<MonthlyStatDTO> monthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        log.info("查看本月汇总（单对象）");
        YearMonth now = YearMonth.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();
        MonthlyStatDTO statDTO = adminService.getMonthlyStats(y, m);
        return Result.success(statDTO);
    }


    /**
     * 刷新每日统计更新daily_stat
     * @param statDate 统计日期（默认昨天）
     */
    @PostMapping("/statistics/daily/refresh")
    public Result<DailyStatDTO> refresh(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate
    ) {
        log.info("刷新每日统计更新daily_stat");
        // 默认昨天
        if (statDate == null) {
            statDate = LocalDate.now().minusDays(1);
        }

        // 不能统计今天（数据还没结束）
        if (!statDate.isBefore(LocalDate.now())) {
            return Result.error("统计日期不能晚于今天");
        }

        DailyStatDTO statDTO = dailyStatService.refresh(statDate);
        return Result.success(statDTO);
    }


    /*
        分页条件查询用户列表
     */
    @GetMapping("/users")
    public Result<PageResult<UserDTO>> searchUsersList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        log.info("分页条件查询用户列表");
        PageResult<UserDTO> result = adminUserService.listUsers(keyword, status, role, page, size);
        return Result.success(result);
    }

    /**
     * 8.2.2 查看单个用户详情
     */
    @GetMapping("/users/{userId}")
    public Result<UserDTO> getUserDetail(@PathVariable Long userId) {
        log.info("查看用户详情：{}", userId);
        UserDTO userDetail = adminUserService.getUserDetail(userId);
        return Result.success(userDetail);
    }

    /**
     * 8.2.3 更新用户基本资料（含角色、状态）
     */
    @PutMapping("/users/{userId}")
    public Result<UserDTO> updateUser(@PathVariable Long userId,
                                          @RequestBody UserDTO request) {
        log.info("更新用户基本资料：{}", userId);
        UserDTO user = adminUserService.updateUser(userId, request);
        return Result.success(user);
    }

    /**
     * 8.2.4 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public Result<String> deleteUser(@PathVariable Long userId) {
        log.info("删除用户：{}", userId);
        adminUserService.deleteUser(userId);
        return Result.success();
    }

    /**
     * 8.2.5 修改用户密码
     */
    @PutMapping("/users/{userId}/password")
    public Result<String> resetPassword(@PathVariable Long userId,
                                        @RequestBody Map<String, String> body) {
        log.info("修改用户密码：{}", userId);
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码不能为空且长度至少6位");
        }
        adminUserService.resetUserPassword(userId, newPassword);
        return Result.success("密码重置成功");
    }

    /**
     * 8.3.1 查看所有笔记
     */
    @GetMapping("/notes")
    public Result<PageResult<DocumentDTO>> listNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "all") String auditStatus,
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("查看所有笔记");
        PageResult<DocumentDTO> result = adminService.listNotes(
                keyword, type, auditStatus, kbId, creatorId, startTime, endTime, page, size);
        return Result.success(result);
    }

    /*
        8.3.2 查看笔记详情
     */
    @GetMapping("/notes/{noteId}")
    public Result<DocumentDTO> getNoteDetail(@PathVariable Long noteId) {
        log.info("查看笔记详情：{}", noteId);
        DocumentDTO documentDTO = adminService.getNoteDetail(noteId);
        return Result.success(documentDTO);
    }

    /**
     * 8.3.3 编辑任意笔记（管理员强制修改，无权限校验）
     */
    @PutMapping("/notes/{noteId}")
    @OpLog(operation = "ADMIN_EDIT_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<DocumentDTO> editNote(@PathVariable Long noteId,
                                              @RequestBody Map<String, Object> body) {
        DocumentDTO documentDTO = adminService.adminEditNote(noteId, body);
        return Result.success(documentDTO);
    }

    /**
     * 8.3.4 审核笔记
     */
    @PutMapping("/notes/{noteId}/audit")
    @OpLog(operation = "AUDIT_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<String> auditNote(@PathVariable Long noteId,
                                    @RequestBody AuditNoteRequest request) {
        adminService.auditNote(noteId, request);
        return Result.success();
    }

    /**
     * 8.3.5 获取知识库列表
     */
    @GetMapping("/kbs")
    public Result<PageResult<KBDTO>> getKBsList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isPublic,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        PageResult<KBDTO> result = adminService.listKbs(keyword, isPublic, creatorId, page, size);
        return Result.success(result);
    }

    /**
     * 8.3.6 查看任意知识库详情
     */
    @GetMapping("/kbs/{kbId}")
    public Result<KBDTO> getKbDetail(@PathVariable Long kbId) {
        KBDTO kbdto = adminService.getKbDetail(kbId);
        return Result.success(kbdto);
    }

    /**
     * 8.3.7 删除知识库
     */
    @DeleteMapping("/kbs/{kbId}")
    @OpLog(operation = "ADMIN_DELETE_KB", targetType = "KB", targetId = "#kbId")
    public Result<String> deleteKb(@PathVariable Long kbId) {
        adminService.deleteKb(kbId);
        return Result.success();
    }

    /**
     * 手动触发刷新 user_stat（仅 ADMIN 角色）
     */
    @PostMapping("/statistics/user/refresh")
    @PreAuthorize("hasRole('ADMIN')")  // 或根据项目实际权限注解
    public Result<String> manualRefresh() {
        userStatRefreshTask.refreshUserStatistics();
        return Result.success();
    }


    /**
     * 8.4 管理员查询操作日志
     */
    @GetMapping("logs")
    public Result<PageResult<OperationLog>> listLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<OperationLog> result = operationLogService.getLogs(
                userId, userName, operation, startTime, endTime, page, size);
        return Result.success(result);
    }

    /**
     * 8.4.1 删除操作日志
     */
    @DeleteMapping
    @OpLog(operation = "ADMIN_DELETE_LOGS", targetType = "LOG")
    public Result<Map<String, Integer>> deleteLogs(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("ids 不能为空");
        }
        operationLogService.deleteByIds(ids);
        return Result.success();
    }
}