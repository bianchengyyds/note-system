package com.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KbFile {
    private Long id;
    private Long kbId;
    private String fileName;    // 原始文件名
    private String filePath;    // 存储路径
    private String fileType;    // 文件类型：pdf/png/jpg
    private Long fileSize;      // 文件大小
    private LocalDateTime createTime;
}