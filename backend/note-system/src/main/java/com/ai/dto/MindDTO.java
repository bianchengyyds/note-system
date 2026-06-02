package com.ai.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MindDTO extends DocumentDTO {

    private List<Node> nodes;

    @Data
    public static class Node {
        private String id;
        private String title;
        private List<Node> children;   // 递归结构
    }
}