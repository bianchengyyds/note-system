package com.ai.mapper;

import com.ai.dto.EmbedDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmbedMapper {
    // 插入嵌入关系
    int insert(EmbedDTO embedDTO);

    // 根据嵌入关系ID查询嵌入关系
    EmbedDTO getEmbedById(Long embedId);

    // 删除嵌入关系
    void deleteEmbed(Long docId, Long embedId);

    // 根据文档ID查询嵌入关系
    List<EmbedDTO> getEmbeds(Long docId);
}
