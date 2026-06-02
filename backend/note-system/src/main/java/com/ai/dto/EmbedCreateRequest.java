package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一步创建并嵌入组件请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedCreateRequest {
    
    /**
     * 嵌入类型：table, board, mind
     */
    private String embedType;
    
    /**
     * 嵌入位置：如 after-paragraph-5
     */
    private String position;
    
    /**
     * 已存在的笔记ID（可选，若提供则直接嵌入）
     */
    private Long noteId;
    
    /**
     * 新建表格参数（当embedType=table且不提供noteId时使用）
     */
    private NewTableData newTable;
    
    /**
     * 新建画板参数（当embedType=board且不提供noteId时使用）
     */
    private NewBoardData newBoard;
    
    /**
     * 新建思维导图参数（当embedType=mind且不提供noteId时使用）
     */
    private NewMindData newMind;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewTableData {
        private String title;
        private List<TableDTO.Column> columns;
        private List<TableDTO.Row> rows;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewBoardData {
        private String title;
        private List<BoardDTO.Element> elements;
        private String background;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewMindData {
        private String title;
        /**
         * 思维导图数据（使用 mindData 字段，与接口文档一致）
         */
        private List<MindDTO.Node> mindData;
    }
}