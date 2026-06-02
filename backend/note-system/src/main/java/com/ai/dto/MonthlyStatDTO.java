package com.ai.dto;

import lombok.Data;

@Data
public class MonthlyStatDTO {
    private String statMonth;      // "2026-04"
    private Integer totalUsers;
    private Integer newUsers;
    private Integer activeUsers;
    private Integer totalNotes;
    private Integer newNotes;
    private Integer totalKbs;
    private Integer totalComments;
}
