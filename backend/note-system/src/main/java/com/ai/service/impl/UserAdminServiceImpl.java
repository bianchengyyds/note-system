package com.ai.service.impl;

import com.ai.dto.UserDTO;
import com.ai.exception.BusinessException;
import com.ai.mapper.AdminUserMapper;
import com.ai.pojo.PageResult;
import com.ai.service.UserAdminService;
import com.ai.utils.CurrentHolder;
import com.aliyuncs.ram.model.v20150501.UpdateUserRequest;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminServiceImpl implements UserAdminService {
    @Autowired
    private AdminUserMapper adminUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserDTO> listUsers(String keyword, String status, String role, int page, int size) {
        // 启动分页
        PageHelper.startPage(page, size);
        List<UserDTO> list = adminUserMapper.selectUserList(keyword, status, role);
        PageInfo<UserDTO> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, size, list);
    }

    @Override
    public UserDTO getUserDetail(Long userId) {
        return adminUserMapper.selectUserDetail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateUser(Long userId, UserDTO req) {
        // 数据清洗：将空字符串转为 null，避免唯一约束冲突
        if (req.getEmail() != null && req.getEmail().trim().isEmpty()) {
            req.setEmail(null);
        }
        if (req.getPhone() != null && req.getPhone().trim().isEmpty()) {
            req.setPhone(null);
        }
        // 更新基本资料
        int rows = adminUserMapper.updateUserByAdmin(userId, req);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
        // 返回更新后的用户详情
        return adminUserMapper.selectUserDetail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        // 1) 不能删除自己
        Long currentUserId = CurrentHolder.getCurrentId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException("不能删除自己的账号");
        }

        // 2) 检查是否拥有笔记（未删除的）或知识库
        int noteCount = adminUserMapper.countUserNotes(userId);
        int kbCount = adminUserMapper.countUserKnowledgeBases(userId);
        if (noteCount > 0 || kbCount > 0) {
            throw new BusinessException("该用户仍有 " + noteCount + " 篇笔记和 " + kbCount +
                    " 个知识库，请先转移或删除后再操作");
        }

        // 3) 执行物理删除（级联删除关联的 member、comment、favorite 等由数据库外键 CASCADE 完成）
        int rows = adminUserMapper.deleteUser(userId);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId, String newPassword) {
        // 1) 不能重置自己的密码（管理员修改自身应走用户端接口）
        Long currentUserId = CurrentHolder.getCurrentId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException("不能重置自己的密码，请到个人中心修改");
        }

        // 2) 检查用户是否存在
        if (adminUserMapper.selectUserDetail(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        // 3) 加密新密码
        String encodedPwd = passwordEncoder.encode(newPassword);

        // 4) 更新密码
        adminUserMapper.updateUserPassword(userId, encodedPwd);
    }
}