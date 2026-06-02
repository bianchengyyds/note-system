package com.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KbBoard {
    private Long id;
    private Long kbId;
    private Long docId;
    private String boardData; // 看板JSON数据
    private String name;
    private LocalDateTime createTime;
}