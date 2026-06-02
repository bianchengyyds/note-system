package com.ai.mapper;

import com.ai.dto.DocumentVersionDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentVersionMapper {

    // 插入文档版本
    void insert(DocumentVersionDTO ver);

    // 删除文档版本
    void deleteByDocId(@Param("docId") Long docId);

    // 查询文档版本
    List<DocumentVersionDTO> listByDocId(@Param("docId") Long docId);

    // 根据版本id和文档id查询文档版本
    DocumentVersionDTO selectByIdAndDocId(@Param("versionId") Long versionId, @Param("docId") Long docId);

}
