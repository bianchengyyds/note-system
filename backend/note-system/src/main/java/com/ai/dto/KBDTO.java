package com.ai.dto;

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
public class KBDTO {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Long creatorId;
    private String creatorName;
    private Integer isPublic;
    private LocalDateTime createdAt;
    private Integer deleted;

    // --- 列表页额外字段 ---
    private Integer docCount;
    private String role; // 协作知识库中的用户角色

    // --- 详情页额外字段 (没有就是 null) ---
    private List<KBMemberDTO> members;
    private List<DocumentDTO> docTree;

    private Long totalViews;
}