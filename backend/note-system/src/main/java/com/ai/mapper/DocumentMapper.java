package com.ai.mapper;

import com.ai.dto.DocumentDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper {
    // 导入文档到知识库
    void insertKB(DocumentDTO doc);

    // 新建文档
    int addDoc(DocumentDTO documentDTO);

    // 获取文档详情
    DocumentDTO getDocDetailById(Long docId);

    //  文档浏览数加1
    void incrementViewCount(Long docId);

    //  更新 文档
    void updateById(DocumentDTO origin);

    // 获取回收站文档列表
    List<DocumentDTO> selectRecycleList(Long userId);

    // 获取文档列表
    DocumentDTO selectByIdIncludeDeleted(@Param("noteId") Long noteId);

    // 删除文档
    void deleteById(Long id);

    // 根据笔记 ID，查询一条未删除的笔记数据，不做任何登录 / 权限检查
    DocumentDTO selectDocByIdForShare(Long docId);

    // 笔记点赞数加1
    void incrementLikeCount(Long docId);

    // 笔记点赞数减1
    void decrementLikeCount(Long targetId);

}
