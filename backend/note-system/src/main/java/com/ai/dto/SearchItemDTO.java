package com.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchItemDTO {
    // --- 结果标识 ---
    private Long id;
    private String type;              // 类型："DOCUMENT" 或 "KNOWLEDGE_BASE"

    // --- 展示内容 ---
    private String title;             // 标题
    private String content;
    private String highlight;         // 搜索高亮摘要 (前端显示的重点)
    private String coverUrl;          // 封面图 (如果是知识库)

    // --- 归属信息 ---
    private Long kbId;          // 所属知识库ID (仅文档类型有)
    private String kbName;      // 所属知识库名称

    // --- 元数据 ---
    private Long creatorId;
    private String creatorName;
    private LocalDateTime updatedAt;
    private Integer viewCount;
}