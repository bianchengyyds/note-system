package com.ai.service.impl;

import com.ai.dto.DashboardDTO;
import com.ai.dto.DocumentDTO;
import com.ai.dto.KBDTO;
import com.ai.mapper.DashboardMapper;
import com.ai.service.DashboardService;
import com.ai.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public DashboardDTO getDashboardInfo() {
        // 1. 创建最终返回的 DTO 对象
        DashboardDTO dashboardDTO = new DashboardDTO();

        // 2. 设置最近浏览笔记 (调用 Mapper 查询)
        Long userId = CurrentHolder.getCurrentId();
        List<DocumentDTO> recentNotes = dashboardMapper.selectRecentNotes(userId);
        dashboardDTO.setRecentNotes(recentNotes);

        // 3. 设置我的知识库
        List<KBDTO> myKbs = dashboardMapper.selectMyKbs(userId);
        dashboardDTO.setMyKbs(myKbs);

        // 4. 设置推荐知识库 (通常不需要 userId，是全局推荐)
        List<KBDTO> recommendedKbs = dashboardMapper.selectRecommendedKbs();
        dashboardDTO.setRecommendedKbs(recommendedKbs);

        // 5. 返回组装好的数据
        return dashboardDTO;
    }
}
