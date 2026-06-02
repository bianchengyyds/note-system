package com.ai.service.impl;

import com.ai.dto.UserStatisticsDTO;
import com.ai.mapper.UserStatisticsMapper;
import com.ai.service.UserStatisticsService;
import com.ai.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserStatisticsServiceImpl implements UserStatisticsService {
    @Autowired
    private UserStatisticsMapper statisticsMapper;

    @Override
    public UserStatisticsDTO getStatistics() {
        Long userId = CurrentHolder.getCurrentId();
        UserStatisticsDTO statistics = statisticsMapper.getStatistics(userId);
        if(statistics == null){
            statistics = new UserStatisticsDTO();
            // 所有字段已经是 null/int 默认0，但明确设一下更安全
            statistics.setCreatedNotes(0);
            statistics.setCreatedKbs(0);
            statistics.setTotalViews(0);
            statistics.setTotalLikes(0);
            statistics.setReceivedComments(0);
            statistics.setTotalWords(0L);
            statistics.setActiveDays(0);
            statistics.setLastActiveTime(null);
        }
        return statistics;
    }
}
