package com.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchQueryDTO {
    private String keyword;             // 搜索关键词
    private String type;                // 类型：ALL(全部), DOCUMENT, TABLE, BOARD, KNOWLEDGE_BASE
    private Long kbId;                  // 所属知识库ID (仅筛选文档时有效)
    private Long creatorId;             // 创建者ID
    private LocalDateTime startTime;    // 更新时间起始
    private LocalDateTime endTime;      // 更新时间结束
    private Integer page = 1;           // 页码
    private Integer size = 20;          // 每页大小
}