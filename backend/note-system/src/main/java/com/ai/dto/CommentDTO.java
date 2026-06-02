package com.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO {
    // --- 核心字段 ---
    private Long id;
    private String content;           // 评论内容

    // --- 关联信息 ---
    private Long noteId;            // 评论目标ID (文档ID/知识库ID)
    private String targetType;        // 评论目标类型 ("DOC", "KB")
    private Long parentCommentId;            // 父评论ID (回复时用，顶级评论为null)

    // --- 作者信息 (返回时填充) ---
    private Long userId;
    private String userName;
    private String userAvatar;

    // --- 元数据 ---
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Boolean hasLiked;         // 当前用户是否点赞了这条评论

    // --- 列表页嵌套 (回复列表) ---
    private List<ReplyDTO> replies;
}