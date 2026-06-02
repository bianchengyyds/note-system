package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 回收站文档专用 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecycleDocDTO {
    private Long id;
    private String title;
    private String type;
    private String content; // 可返回摘要或截断内容
    private Long kbId;
    private LocalDateTime deletedAt;
    private String creatorName; // 接口文档要求的创建者姓名
}