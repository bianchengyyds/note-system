package com.ai.mapper;

import com.ai.dto.KbMind;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KbMindMapper {
    // 插入思维导图数据
    void insert(KbMind kbMind);

    // 根据文档ID查询思维导图数据
    KbMind selectByDocId(Long id);

    // 根据文档ID更新思维导图数据
    void updateByDocId(KbMind ext);

    // 根据文档ID更新文档ID
    void updateKbIdByDocId(KbMind mindExt);

    // 根据文档ID删除思维导图数据
    void deleteByDocId(@Param("docId") Long docId);
}
