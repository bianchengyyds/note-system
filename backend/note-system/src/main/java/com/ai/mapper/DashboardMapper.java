package com.ai.mapper;

import com.ai.dto.DocumentDTO;
import com.ai.dto.KBDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {
    // 最近浏览的笔记
    List<DocumentDTO> selectRecentNotes(Long userId);

    // 我的知识库
    List<KBDTO> selectMyKbs(Long userId);

    // 推荐知识库
    List<KBDTO> selectRecommendedKbs();
}
