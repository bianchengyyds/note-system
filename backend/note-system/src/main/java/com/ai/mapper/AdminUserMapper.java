package com.ai.mapper;

import com.ai.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminUserMapper {
    // 查询用户列表
    List<UserDTO> selectUserList(String keyword, String status, String role);

    // 查询用户详情
    UserDTO selectUserDetail(Long userId);

    // 更新用户信息
    int updateUserByAdmin(Long userId, UserDTO req);

    // 用户笔记数量
    int countUserNotes(Long userId);

    // 用户知识库数量
    int countUserKnowledgeBases(Long userId);

    // 删除用户
    int deleteUser(Long userId);

    // 修改用户密码
    void updateUserPassword(Long userId, String password);
}
