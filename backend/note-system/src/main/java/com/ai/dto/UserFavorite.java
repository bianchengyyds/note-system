package com.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFavorite {
    private Long id;
    private Long userId;
    private Long targetId;
    private LocalDateTime createdAt;
}
