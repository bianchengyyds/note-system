package com.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardDTO {
    // 最近浏览的笔记
    private List<DocumentDTO> recentNotes;
    // 我的知识库
    private List<KBDTO> myKbs;
    // 推荐的知识库
    private List<KBDTO> recommendedKbs;
}