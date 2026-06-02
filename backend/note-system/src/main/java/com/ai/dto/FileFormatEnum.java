package com.ai.dto;

import lombok.Getter;

/**
 * 支持导入的文件格式枚举
 */
@Getter
public enum FileFormatEnum {
    // 系统自有格式
    LAKEBOOK(".lakebook"),
    LAKE(".lake"),
    LAKEBOARD(".lakeboard"),
    LAKETABLE(".laketable"),

    // 三方文档类
    DOC(".doc"), DOCX(".docx"), MD(".md"), TXT(".txt"), PPT(".ppt"), PPTX(".pptx"),

    // 三方表格类
    XLSX(".xlsx"), XLS(".xls"), CSV(".csv"),

    // 三方预览类
    PDF(".pdf"), JPG(".jpg"), PNG(".png");

    private final String suffix;

    FileFormatEnum(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 校验文件后缀是否支持
     */
    public static boolean isSupported(String suffix) {
        for (FileFormatEnum format : values()) {
            if (format.getSuffix().equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }
}