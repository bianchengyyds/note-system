package com.ai.dto;

import lombok.Data;

@Data
public class AuditNoteRequest {
    private Boolean approved;
    private String reason;  // 驳回时可填写原因，通过时可为空
}