package com.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String operation;
    private String targetType;
    private Long targetId;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;

    // 非持久化字段，用于联表查询时展示用户名
    private String userName;
}