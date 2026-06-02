package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 文件导入结果返回实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultVO {
    // 成功导入数量
    private Integer successCount;
    // 失败导入数量
    private Integer failCount;
    // 失败文件名称列表
    private List<String> failList;
}