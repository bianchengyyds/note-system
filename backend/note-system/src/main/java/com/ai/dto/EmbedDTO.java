package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedDTO {

    private Long embedId;
    private Long docId;
    private String embedType;
    private Long noteId;
    private String noteTitle;
    private String embedData;
    private String position;
    private LocalDateTime createdAt;
    
    /**
     * 组件完整内容数据（当 includeData=true 时返回）
     */
    private Map<String, Object> content;
}