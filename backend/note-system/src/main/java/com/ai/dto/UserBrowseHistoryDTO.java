package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBrowseHistoryDTO {
    private Long id;
    private Long userId;
    private Long noteId;
    private String title;
    private String type;
    private String content;
    private String kbName;
    private LocalDateTime browseTime;
}
