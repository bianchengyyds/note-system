package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一步创建并嵌入组件响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedCreateResponse {
    
    private Long embedId;
    private Long docId;
    private String embedType;
    private Long noteId;
    private String position;
    private LocalDateTime createdAt;
    
    /**
     * 新创建笔记的完整信息
     */
    private DocumentDTO note;
}