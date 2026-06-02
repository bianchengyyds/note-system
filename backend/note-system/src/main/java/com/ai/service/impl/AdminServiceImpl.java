package com.ai.service.impl;

import com.ai.dto.*;
import com.ai.exception.BusinessException;
import com.ai.mapper.AdminMapper;
import com.ai.mapper.DocumentVersionMapper;
import com.ai.mapper.UserStatisticsMapper;
import com.ai.pojo.PageResult;
import com.ai.service.AdminService;
import com.ai.utils.CurrentHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private DocumentVersionMapper documentVersionMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public AdminOverviewDTO getOverview() {
        AdminOverviewDTO overview = adminMapper.getOverview();
        // 防止 null，设置默认值
        if (overview == null) {
            overview = new AdminOverviewDTO();
            overview.setTotalUsers(0);
            overview.setNewUsersToday(0);
            overview.setActiveUsersToday(0);
            overview.setTotalNotes(0);
            overview.setTotalKbs(0);
            overview.setTotalComments(0);
        }
        return overview;
    }

    @Override
    public List<MonthlyStatDTO> getYearlyStats(int year) {
        return adminMapper.selectYearlyStats(year);
    }

    @Override
    public MonthlyStatDTO getMonthlyStats(int year, int month) {
        return adminMapper.selectMonthlyStats(year, month);
    }

    @Override
    public PageResult<DocumentDTO> listNotes(String keyword, String type, String auditStatus,
                                             Long kbId, Long creatorId,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             int page, int size) {
        PageHelper.startPage(page, size);
        List<DocumentDTO> list = adminMapper.selectNoteList(keyword, type, auditStatus,
                kbId, creatorId, startTime, endTime);
        PageInfo<DocumentDTO> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, size, list);
    }

    @Override
    public DocumentDTO getNoteDetail(Long noteId) {
        DocumentDTO documentDTO = adminMapper.selectNoteDetail(noteId);
        if (documentDTO == null) {
            throw new BusinessException("笔记不存在");
        }
        return documentDTO;
    }

    @Override
    @Transactional
    public DocumentDTO adminEditNote(Long noteId, Map<String, Object> body) {
        // 1. 查询笔记存在性（不受 deleted 限制）
        DocumentDTO note = adminMapper.selectNoteDetail(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 2. 保存旧版本快照
        DocumentVersionDTO version = new DocumentVersionDTO();
        version.setDocId(note.getId());
        version.setVersion(note.getVersion());
        version.setTitle(note.getTitle());
        version.setContent(note.getContent());
        version.setEditorId(CurrentHolder.getCurrentId());
        version.setCreatedAt(LocalDateTime.now());
        documentVersionMapper.insert(version);

        // 3. 更新基本字段（title）
        String newTitle = (String) body.getOrDefault("title", note.getTitle());
        note.setTitle(newTitle);

        // 4. 根据类型更新扩展字段（关键：这些方法会直接修改 note 对象的 content 字段）
        String type = note.getType();
        switch (type) {
            case "doc":
                if (body.containsKey("content")) {
                    note.setContent((String) body.get("content"));
                }
                break;
            case "table":
                updateTableNote(note, body);  // 【修复】直接传入 note 对象
                break;
            case "board":
                updateBoardNote(note, body);  // 【修复】直接传入 note 对象
                break;
            case "mind":
                updateMindNote(note, body);   // 【修复】直接传入 note 对象
                break;
            default:
                throw new BusinessException("不支持的笔记类型: " + type);
        }

        // 5. 更新 document 基础信息（版本号+1，更新时间，以及被扩展方法修改过的 content）
        note.setVersion(note.getVersion() + 1);
        note.setLastEditorId(CurrentHolder.getCurrentId());
        note.setUpdatedAt(LocalDateTime.now());
        adminMapper.updateDocument(note);  // 【关键】这里会统一更新所有字段，包括 content

        // 6. 返回最新的管理员详情
        return adminMapper.selectNoteDetail(noteId);
    }

    // 更新表格扩展数据
    private void updateTableNote(DocumentDTO note, Map<String, Object> body) {
        // 仅当传递了columns或rows时才更新
        if (!body.containsKey("columns") && !body.containsKey("rows")) {
            return;
        }

        KbTable table = adminMapper.selectKbTableByDocId(note.getId());
        if (table == null) {
            table = new KbTable();
            table.setDocId(note.getId());
            table.setKbId(note.getKbId());
        }

        // 解析现有数据
        TableDTO temp = parseTableData(table.getTableData());

        boolean hasDataChange = false;
        if (body.containsKey("columns")) {
            temp.setColumns(convertValue(body.get("columns"), new com.fasterxml.jackson.core.type.TypeReference<List<TableDTO.Column>>() {}));
            hasDataChange = true;
        }
        if (body.containsKey("rows")) {
            temp.setRows(convertValue(body.get("rows"), new com.fasterxml.jackson.core.type.TypeReference<List<TableDTO.Row>>() {}));
            hasDataChange = true;
        }

        // 【关键修复】无论是否有数据变化，都要更新标题
        table.setName(note.getTitle());
        
        if (hasDataChange) {
            String newExtJson = toJson(new TableDTO(temp.getColumns(), temp.getRows()));
            table.setTableData(newExtJson);
            // 直接修改 note 对象的 content
            note.setContent(newExtJson);
        }

        // 【关键修复】无论是否有数据变化，都要调用更新方法
        if (table.getId() == null) {
            adminMapper.insertKbTable(table);
        } else {
            adminMapper.updateKbTable(table);  // 这里会更新 name 字段
        }
    }

    // 更新画板扩展数据
    private void updateBoardNote(DocumentDTO note, Map<String, Object> body) {
        // 仅当传递了elements或background时才更新
        if (!body.containsKey("elements") && !body.containsKey("background")) {
            return;
        }

        KbBoard board = adminMapper.selectKbBoardByDocId(note.getId());
        if (board == null) {
            board = new KbBoard();
            board.setDocId(note.getId());
            board.setKbId(note.getKbId());
        }

        // 解析现有数据
        BoardDTO temp = parseBoardData(board.getBoardData());

        boolean hasDataChange = false;
        if (body.containsKey("elements")) {
            temp.setElements(convertValue(body.get("elements"), new com.fasterxml.jackson.core.type.TypeReference<List<BoardDTO.Element>>() {}));
            hasDataChange = true;
        }
        if (body.containsKey("background")) {
            temp.setBackground((String) body.get("background"));
            hasDataChange = true;
        }

        // 【关键修复】无论是否有数据变化，都要更新标题
        board.setName(note.getTitle());
        
        if (hasDataChange) {
            String newExtJson = toJson(new BoardDTO(temp.getElements(), temp.getBackground()));
            board.setBoardData(newExtJson);
            // 直接修改 note 对象的 content
            note.setContent(newExtJson);
        }

        // 【关键修复】无论是否有数据变化，都要调用更新方法
        if (board.getId() == null) {
            adminMapper.insertKbBoard(board);
        } else {
            adminMapper.updateKbBoard(board);  // 这里会更新 name 字段
        }
    }

    // 更新思维导图扩展数据
    private void updateMindNote(DocumentDTO note, Map<String, Object> body) {
        // 兼容 nodes 和 mindData 字段
        if (!body.containsKey("nodes") && !body.containsKey("mindData")) {
            return;
        }

        KbMind mind = adminMapper.selectKbMindByDocId(note.getId());
        if (mind == null) {
            mind = new KbMind();
            mind.setDocId(note.getId());
            mind.setKbId(note.getKbId());
        }

        // 解析现有数据
        List<MindDTO.Node> nodes = parseMindData(mind.getMindData());

        boolean hasDataChange = false;
        if (body.containsKey("nodes")) {
            nodes = convertValue(body.get("nodes"), new com.fasterxml.jackson.core.type.TypeReference<List<MindDTO.Node>>() {});
            hasDataChange = true;
        } else if (body.containsKey("mindData")) {
            nodes = convertValue(body.get("mindData"), new com.fasterxml.jackson.core.type.TypeReference<List<MindDTO.Node>>() {});
            hasDataChange = true;
        }

        // 【关键修复】无论是否有数据变化，都要更新标题
        mind.setName(note.getTitle());
        
        if (hasDataChange) {
            String newExtJson = toJson(nodes);
            mind.setMindData(newExtJson);
            // 直接修改 note 对象的 content
            note.setContent(newExtJson);
        }

        // 【关键修复】无论是否有数据变化，都要调用更新方法
        if (mind.getId() == null) {
            adminMapper.insertKbMind(mind);
        } else {
            adminMapper.updateKbMind(mind);  // 这里会更新 name 字段
        }
    }

    // ========== 辅助方法 ==========
    
    private TableDTO parseTableData(String json) {
        try {
            TableDTO dto = objectMapper.readValue(json, TableDTO.class);
            return dto != null ? dto : new TableDTO();
        } catch (Exception e) {
            throw new RuntimeException("表格数据解析失败", e);
        }
    }

    private BoardDTO parseBoardData(String json) {
        try {
            BoardDTO dto = objectMapper.readValue(json, BoardDTO.class);
            return dto != null ? dto : new BoardDTO();
        } catch (Exception e) {
            throw new RuntimeException("看板数据解析失败", e);
        }
    }

    private List<MindDTO.Node> parseMindData(String json) {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        try {
            // 先尝试解析为 JsonNode 判断类型
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(json);
            
            // 如果是数组，直接解析
            if (rootNode.isArray()) {
                List<MindDTO.Node> nodes = objectMapper.readValue(json, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<MindDTO.Node>>() {});
                return nodes != null ? nodes : new java.util.ArrayList<>();
            }
            // 如果是对象，尝试提取 "nodes" 字段
            else if (rootNode.has("nodes")) {
                String nodesJson = rootNode.get("nodes").toString();
                List<MindDTO.Node> nodes = objectMapper.readValue(nodesJson, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<MindDTO.Node>>() {});
                return nodes != null ? nodes : new java.util.ArrayList<>();
            }
            // 其他情况返回空列表
            else {
                return new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 解析失败时返回空列表，而不是抛异常
            return new java.util.ArrayList<>();
        }
    }

    private <T> T convertValue(Object from, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) {
        return objectMapper.convertValue(from, typeRef);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("数据序列化失败", e);
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditNote(Long noteId, AuditNoteRequest request) {
        DocumentDTO note = adminMapper.selectNoteDetail(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 更新审核状态
        Integer newStatus = request.getApproved() ? 1 : 2; // 1通过，2驳回
        note.setAuditStatus(newStatus);
        adminMapper.updateAuditStatus(noteId, newStatus);
    }

    @Override
    public PageResult<KBDTO> listKbs(String keyword, Integer isPublic, Long creatorId, int page, int size) {
        PageHelper.startPage(page, size);
        List<KBDTO> list = adminMapper.selectKbList(keyword, isPublic, creatorId);
        PageInfo<KBDTO> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, size, list);
    }

    @Override
    public KBDTO getKbDetail(Long kbId) {
        // 1. 基本信息
        KBDTO kbdto = adminMapper.selectKbDetailBasic(kbId);
        if (kbdto == null) {
            throw new BusinessException("知识库不存在");
        }

        // 2. 成员列表
        List<KBMemberDTO> members = adminMapper.selectKbMembers(kbId);
        kbdto.setMembers(members);

        // 3. 笔记树（扁平转树）
        List<DocumentDTO> flatList = adminMapper.selectKbNotesFlat(kbId);
        List<DocumentDTO> tree = buildNoteTree(flatList);
        kbdto.setDocTree(tree);

        return kbdto;
    }
    /** 将扁平笔记列表构建为树形结构 */
    private List<DocumentDTO> buildNoteTree(List<DocumentDTO> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();

        Map<Long, DocumentDTO> nodeMap = items.stream()
                .collect(Collectors.toMap(DocumentDTO::getId, Function.identity(), (a, b) -> a));

        List<DocumentDTO> roots = new ArrayList<>();
        for (DocumentDTO node : items) {
            if (node.getParentDocId() == null) {
                roots.add(node);
            } else {
                DocumentDTO parent = nodeMap.get(node.getParentDocId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    @Override
    public void deleteKb(Long kbId) {
        // 1. 检查知识库是否存在（不过滤 deleted）
        KBDTO kb = adminMapper.selectKbDetailBasic(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        // 2. 执行物理删除
        adminMapper.deleteKb(kbId);
    }
}
