package com.ai.dto;

import com.ai.pojo.Document;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 忽略 null 字段
public class DocumentDTO {
    // --- 核心字段 ---
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long kbId;
    private String kbName;
    // 强制显示该字段，无论是否为null
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Long parentDocId;
    private List<DocumentDTO> children;

    // --- 元数据 ---
    private Long creatorId;
    private Long lastEditorId;
    private Integer version;
    private Integer isPublic;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer auditStatus;      // 0待审核 1通过 2驳回
    private LocalDateTime deletedAt;  // 回收站删除时间

    // --- 详情页额外状态 ---
    private Boolean hasLiked;
    private Boolean hasFavorited;

    // --- 移动文档用 ---
    private Long targetKbId;
    private Long targetParentDocId;

    private LocalDateTime lastViewTime;
}