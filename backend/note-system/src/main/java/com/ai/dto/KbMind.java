package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KbMind {
    private Long id;
    private Long kbId;
    private Long docId;
    private String mindData; // JSON数据
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}