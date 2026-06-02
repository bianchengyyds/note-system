package com.ai.dto;

import lombok.Getter;

@Getter
public enum TargetType {
    DOC,    // 统一代表：文档、表格、看板、思维导图（全部归属主表）
    COMMENT // 评论（独立区分）
}