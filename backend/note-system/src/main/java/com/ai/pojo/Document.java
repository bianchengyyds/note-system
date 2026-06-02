package com.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private Integer id;
    private String title;
    private String content;
    private String type;
    private Integer kbId;
    private Integer parentDocId;
    private Integer creatorId;
    private Integer lastEditorId;
    private Integer version;
    private Integer isPublic;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private String createdAt;
    private String updatedAt;

    private List<Document> children;

    private Boolean hasLiked;
    private Boolean hasFavorited;
}
