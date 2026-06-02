package com.ai.service;

import com.ai.dto.UserDTO;
import com.ai.pojo.PageResult;

public interface UserAdminService {
    PageResult<UserDTO> listUsers(String keyword, String status, String role, int page, int size);

    UserDTO getUserDetail(Long userId);

    UserDTO updateUser(Long userId, UserDTO request);

    void deleteUser(Long userId);

    void resetUserPassword(Long userId, String newPassword);
}
