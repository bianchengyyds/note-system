package com.ai.mapper;

import com.ai.dto.KbBoard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 2. KbBoardMapper
public interface KbBoardMapper {
    // 新增
    void insert(KbBoard board);

    // 根据文档ID查询
    KbBoard selectByDocId(Long id);

    // 根据文档ID更新
    void updateByDocId(KbBoard ext);

    // 根据文档ID更新KBID
    void updateKbIdByDocId(KbBoard boardExt);

    // 根据文档ID删除
    void deleteByDocId(@Param("docId") Long docId);
}
