package com.ai.mapper;

import com.ai.dto.UserBrowseHistoryDTO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBrowseHistoryMapper {

    // 根据用户ID查询浏览历史
    Page<UserBrowseHistoryDTO> selectBrowseHistoriesByUserId(Long userId);

    // 插入浏览历史
    void insert(UserBrowseHistoryDTO history);
}
