package com.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BoardDTO extends DocumentDTO {

    private List<Element> elements;
    private String background;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Element {
        private String id;
        private String type;      // rect / line / circle / text / i-text 等
        private Integer x;
        private Integer y;
        private Integer left;     // 兼容 Fabric.js 的坐标
        private Integer top;      // 兼容 Fabric.js 的坐标
        private Integer width;
        private Integer height;
        private String content;
        // 线条特有属性
        private Integer startX;
        private Integer startY;
        private Integer endX;
        private Integer endY;

        private String fill;
        private String stroke;
        private Integer strokeWidth;
        private String fontSize;
        private String fontFamily;
        private String text;
        private Integer radius;
    }
}