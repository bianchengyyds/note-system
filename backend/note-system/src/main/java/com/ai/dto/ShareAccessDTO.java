package com.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShareAccessDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;                // 笔记ID
    private String title;           // 笔记标题
    private String content;         // 笔记内容
    private String type;            // 笔记类型（doc/table/board/mind）
    private String creatorName;     // 创建者名称
    private LocalDateTime createdAt;// 创建时间
    private String permission;      // 分享权限（READ/EDIT）
}