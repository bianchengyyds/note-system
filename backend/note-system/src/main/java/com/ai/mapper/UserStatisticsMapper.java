package com.ai.mapper;

import com.ai.dto.AdminOverviewDTO;
import com.ai.dto.UserStatisticsDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserStatisticsMapper {
    // 获取用户统计信息
    UserStatisticsDTO getStatistics(Long userId);

}
