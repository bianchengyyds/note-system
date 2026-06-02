package com.ai.mapper;

import com.ai.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    // 根据用户名统计数量
    Integer countByName(@Param("name") String name);

    // 根据邮箱统计数量
    Integer countByEmail(@Param("email") String email);

    // 根据手机号统计数量
    Integer countByPhone(@Param("phone") String phone);

    // 插入用户
    Integer insertUser(UserDTO userDTO);

    // 根据用户名查询用户
    UserDTO selectUserByName(@Param("name") String name);

    // 方法1：登录专用 - 仅更新登录时间
    Integer updateLoginTime(UserDTO userDTO);

    // 根据ID查询用户
    UserDTO getUserById(Long userId);

    // 方法2：修改资料专用 - 动态更新非 null 字段
    Integer updateUserProfile(UserDTO userDTO);

    // 修改密码
    void updatePassword(Long userId, String encodedNewPassword);

    // 根据ID查询用户名
    String selectNameById(@Param("userId") Long userId);

    // 搜索用户
    @Select("SELECT id, name FROM user WHERE name LIKE CONCAT('%', #{keyword}, '%') LIMIT 10")
    List<UserDTO> searchUsers(@Param("keyword") String keyword);
}