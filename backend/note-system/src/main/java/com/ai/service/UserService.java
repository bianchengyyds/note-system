package com.ai.service;

import com.ai.dto.UserBrowseHistoryDTO;
import com.ai.dto.UserDTO;
import com.ai.dto.UserFavoriteDTO;
import com.ai.pojo.LogInfo;
import com.ai.pojo.PageResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserDTO getUserById(Long userId);

    UserDTO updateProfile(UserDTO userDTO);

    void updataPassword(UserDTO userDTO);

    PageResult<LogInfo> getUserLogById(Integer page, Integer size);


    PageResult<UserFavoriteDTO> getFavoritesList(Integer page, Integer size);

    PageResult<UserBrowseHistoryDTO> getBrowseHistory(Integer page, Integer size);

    List<UserDTO> searchUsers(String keyword);
}