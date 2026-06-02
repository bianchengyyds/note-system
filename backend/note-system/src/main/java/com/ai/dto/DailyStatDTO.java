package com.ai.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DailyStatDTO {
    private LocalDate statDate;
    private Integer totalUsers;
    private Integer newUsers;
    private Integer activeUsers;
    private Integer totalNotes;
    private Integer newNotes;
    private Integer totalKbs;
    private Integer newKbs;
    private Integer totalComments;
}