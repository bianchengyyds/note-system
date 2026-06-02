package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionDTO {
    private Long id;            // 版本记录 ID
    private Long docId;         // 关联的文档 ID
    private Integer version;    // 版本号
    private String title;       // 该版本的标题
    private String content;     // 该版本的内容快照（doc 类型为 Markdown，其他类型为 JSON 或 NULL）
    private Long editorId;      // 编辑者用户 ID
    private String editorName;  // 编辑者名称（非数据库字段，联表查询时填充）
    private LocalDateTime createdAt;  // 版本创建时间
}