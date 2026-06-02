package com.ai.mapper;

import com.ai.dto.SearchItemDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchMapper {

    List<SearchItemDTO> searchDocuments(@Param("keyword") String keyword,
                                        @Param("type") String type,
                                        @Param("kbId") Long kbId,
                                        @Param("creatorId") Long creatorId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    List<SearchItemDTO> searchKnowledgeBases(@Param("keyword") String keyword,
                                             @Param("creatorId") Long creatorId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}