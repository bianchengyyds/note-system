package com.ai.service;

import com.ai.dto.*;
import com.ai.pojo.PageResult;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface DocumentService {
    DocumentDTO addDoc(DocumentDTO documentDTO);

    TableDTO addTable(TableDTO tableDTO);

    BoardDTO addBoard(BoardDTO boardDTO);

    MindDTO addMind(MindDTO mindDTO);

    DocumentDTO getDocDetailById(Long docId);

    DocumentDTO updateNote(Long noteId, Map<String, Object> body);

    DocumentDTO moveDoc(Long noteId, Long targetKbId, Long targetParentDocId);

    void deleteDoc(Long noteId);

    PageResult<RecycleDocDTO> getRecycleDocs(Integer page, Integer size);

    DocumentDTO restoreDoc(Long noteId);

    void deleteDocPermanently(Long noteId);


    List<DocumentVersionDTO> getDocVersions(Long noteId);

    DocumentDTO rollbackDoc(Long noteId, Long versionId);

    ShareDTO shareDoc(Long noteId, ShareDTO shareDTO);

    ShareAccessDTO accessByShareKey(String shareKey, String password);

    ResponseEntity<Resource> exportNote(Long noteId, ExportFormatEnum formatEnum);

    void recordView(Long noteId);

    List<CommentDTO> getDocCommentsList(Long noteId);

    DocumentDTO getVersionDetail(Long noteId, Long versionId);
}
