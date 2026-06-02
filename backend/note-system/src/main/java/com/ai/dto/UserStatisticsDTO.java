package com.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserStatisticsDTO {
    private Integer createdNotes;
    private Integer createdKbs;
    private Integer totalViews;
    private Integer totalLikes;
    private Integer receivedComments;
    private Long totalWords;          // 字数可能很大，用 Long
    private Integer activeDays;
    private LocalDateTime lastActiveTime;
}