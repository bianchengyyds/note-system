package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // NoteShare 表中的全部属性
    private Long id;
    private Long docId;
    private String shareKey;
    private String password;          // 加密存储的密码
    private LocalDateTime expireTime; // 过期时间（null表示永久）
    private String permission;        // READ/EDIT
    private Long creatorId;           // 分享者ID
    private LocalDateTime createdAt;  // 创建时间

    // 辅助字段：接收用户选择的分享时长（不存数据库）
    // 可选值："HALF_YEAR"（半年）、"PERMANENT"（永久）
    private transient String shareDuration;

    // 返回分享链接（transient 不参与序列化）
    private transient String shareUrl;
}