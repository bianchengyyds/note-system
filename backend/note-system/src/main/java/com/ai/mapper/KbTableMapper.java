package com.ai.mapper;

import com.ai.dto.KbTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.security.core.parameters.P;

@Mapper
// 3. KbTableMapper.xml
public interface KbTableMapper {
    // 插入表格数据
    void insert(KbTable table);

    // 根据文档ID查询表格数据
    KbTable selectByDocId(Long docId);

    // 根据文档ID更新表格数据
    void updateByDocId(KbTable ext);

    // 根据文档ID更新kbId
    void updateKbIdByDocId(KbTable tableExt);

    // 根据文档ID删除表格数据
    void deleteByDocId(@Param("docId") Long docId);
}