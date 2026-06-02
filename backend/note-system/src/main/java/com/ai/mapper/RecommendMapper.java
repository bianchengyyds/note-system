package com.ai.mapper;

import com.ai.dto.DocumentDTO;
import com.ai.dto.KBDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RecommendMapper {
    // 查询热门笔记
    List<DocumentDTO> selectHotNotes(Integer limit);

    // 查询热门知识库
    List<KBDTO> selectHotKbs(Integer limit);
}
