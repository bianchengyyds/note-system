package com.ai.mapper;

import com.ai.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMapper {
    // 获取管理员总览信息
    AdminOverviewDTO getOverview();

    // 获取指定年份的月度统计信息
    List<MonthlyStatDTO> selectYearlyStats(int year);

    // 获取指定年月的月度统计信息
    MonthlyStatDTO selectMonthlyStats(int year, int month);

    // 获取笔记列表
    List<DocumentDTO> selectNoteList(String keyword, String type, String auditStatus, Long kbId, Long creatorId, LocalDateTime startTime, LocalDateTime endTime);

    // 获取笔记详情
    DocumentDTO selectNoteDetail(Long noteId);

    // 更新 document 表（动态字段需在 XML 处理，这里假设全量更新）
    void updateDocument(DocumentDTO documentDTO);

    // 表格扩展
    KbTable selectKbTableByDocId(@Param("docId") Long docId);
    void insertKbTable(KbTable table);
    void updateKbTable(KbTable table);

    // 画板扩展
    KbBoard selectKbBoardByDocId(@Param("docId") Long docId);
    void insertKbBoard(KbBoard board);
    void updateKbBoard(KbBoard board);

    // 思维导图扩展
    KbMind selectKbMindByDocId(@Param("docId") Long docId);
    void insertKbMind(KbMind mind);
    void updateKbMind(KbMind mind);

    // 审核笔记
    void updateAuditStatus(Long noteId, Integer auditStatus);

    // 获取知识库列表
    List<KBDTO> selectKbList(String keyword, Integer isPublic, Long creatorId);

    // 获取知识库详情
    KBDTO selectKbDetailBasic(Long kbId);

    // 获取知识库成员列表
    List<KBMemberDTO> selectKbMembers(Long kbId);

    // 查询知识库下所有未删除笔记（扁平列表，用于构建树）
    List<DocumentDTO> selectKbNotesFlat(Long kbId);

    // 删除知识库
    void deleteKb(Long kbId);
}
