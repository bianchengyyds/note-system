package com.ai.dto;

import lombok.Data;

@Data
public class AdminOverviewDTO {
    private Integer totalUsers;
    private Integer newUsersToday;
    private Integer activeUsersToday;
    private Integer totalNotes;
    private Integer totalKbs;
    private Integer totalComments;
}