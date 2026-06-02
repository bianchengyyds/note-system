package com.ai.service;

import com.ai.dto.EmbedCreateRequest;
import com.ai.dto.EmbedCreateResponse;
import com.ai.dto.EmbedDTO;

import java.util.List;

public interface EmbedService {
    EmbedDTO insertEmbed(Long docId, EmbedDTO embed);

    /**
     * 一步创建并嵌入组件
     * 当请求体中不提供 noteId，而是提供对应组件类型的新建数据字段时，
     * 后端自动创建该组件笔记，然后将其嵌入到文档中
     */
    EmbedCreateResponse createAndEmbed(Long docId, EmbedCreateRequest request);

    void deleteEmbed(Long docId, Long embedId);

    /**
     * 获取文档中的嵌套组件列表
     * @param docId 文档ID
     * @param includeData 是否返回完整内容数据
     * @return 嵌入组件列表
     */
    List<EmbedDTO> getEmbeds(Long docId, boolean includeData);
}