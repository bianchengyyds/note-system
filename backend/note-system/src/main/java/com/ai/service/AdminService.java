package com.ai.service;

import com.ai.dto.*;
import com.ai.pojo.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminService {
    AdminOverviewDTO getOverview();


    List<MonthlyStatDTO> getYearlyStats(int y);

    MonthlyStatDTO getMonthlyStats(int y, int m);

    PageResult<DocumentDTO> listNotes(String keyword, String type, String auditStatus, Long kbId, Long creatorId, LocalDateTime startTime, LocalDateTime endTime, int page, int size);

    DocumentDTO getNoteDetail(Long noteId);

    DocumentDTO adminEditNote(Long noteId, Map<String, Object> body);

    void auditNote(Long noteId, AuditNoteRequest request);

    PageResult<KBDTO> listKbs(String keyword, Integer isPublic, Long creatorId, int page, int size);


    KBDTO getKbDetail(Long kbId);

    void deleteKb(Long kbId);
}
