package com.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)  // 若继承 DocumentDTO
public class TableDTO extends DocumentDTO {
    private List<Column> columns;
    private List<Row> rows;

    // 内部类定义列与行结构
    @Data
    public static class Column {
        private String key;
        private String title;
        private String type;  // text/number/date...
    }

    @Data
    public static class Row {
        private String id;
        private Map<String, Object> cells;
    }
}