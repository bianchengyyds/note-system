package com.ai.dto;

import lombok.Data;

@Data
public class FileDTO {
    // --- 核心信息 ---
    private String url;                 // 文件访问地址 (最常用)
    private String filename;            // 原始文件名

    // --- 扩展信息 ---
    private Long size;                  // 文件大小 (字节)
    private String fileType;            // 文件类型 ("IMAGE", "PDF", "DOC"...)
    private String extension;           // 后缀名

    // --- 上传场景专用 (如果是前端直传OSS) ---
    private String uploadId;            // 上传ID
    private String uploadUrl;           // 临时上传地址
}