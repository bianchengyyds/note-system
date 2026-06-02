package com.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogInfo {
    private Integer id;
    private String operation;
    private String targetType;
    private Integer targetId;
    private String ip;
    private String userAgent;
    private String createdAt;
}
