package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 笔记导出格式枚举
 */
@Getter
@AllArgsConstructor
public enum ExportFormatEnum {

    WORD("word", ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "doc"),
    MARKDOWN("markdown", ".md", "text/markdown", "doc"),
    PDF("pdf", ".pdf", "application/pdf", "doc,table,board"),
    LAKE("lake", ".lake", "application/octet-stream", "doc"),
    JPG("jpg", ".jpg", "image/jpeg", "doc,board"),
    XLSX("xlsx", ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "table");

    /**
     * 路径参数值
     */
    private final String code;
    /**
     * 文件后缀
     */
    private final String suffix;
    /**
     * 响应Content-Type
     */
    private final String contentType;
    /**
     * 支持的笔记类型 doc/table/board/mind
     */
    private final String supportTypes;

    /**
     * 根据code获取枚举
     */
    public static ExportFormatEnum getByCode(String code) {
        for (ExportFormatEnum format : values()) {
            if (format.getCode().equals(code)) {
                return format;
            }
        }
        return null;
    }
}