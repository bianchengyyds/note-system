package com.ai.service.impl;

import com.ai.dto.*;
import com.ai.exception.BusinessException;
import com.ai.mapper.EmbedMapper;
import com.ai.mapper.KBMemberMapper;
import com.ai.service.DocumentService;
import com.ai.service.EmbedService;
import com.ai.service.KBService;
import com.ai.utils.CurrentHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EmbedServiceImpl implements EmbedService {
    @Autowired
    private EmbedMapper embedMapper;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private KBService kbService;
    @Autowired
    private KBMemberMapper kbMemberMapper;
    @Autowired
    private com.ai.mapper.UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmbedDTO insertEmbed(Long docId, EmbedDTO embed) {
        // 1. 业务校验：检查被嵌入的 noteId 是否存在
        if (documentService.getDocDetailById(embed.getNoteId()) == null) {
            throw new BusinessException("被嵌入的笔记不存在");
        }
        // 2. 业务校验：检查被嵌入的文档是否存在
        if(documentService.getDocDetailById(docId) == null){
            throw new BusinessException("文档不存在");
        } else if (!documentService.getDocDetailById(docId).getType().equals("doc")) {
            throw new BusinessException("嵌入的笔记只能是文档类型");
        }


        // 3. 组装实体并入库
        EmbedDTO embedDTO = new EmbedDTO();
        embedDTO.setDocId(docId);
        embedDTO.setEmbedType(embed.getEmbedType());
        embedDTO.setNoteId(embed.getNoteId());
        embedDTO.setPosition(embed.getPosition());
        embedDTO.setCreatedAt(LocalDateTime.now());

        int rows = embedMapper.insert(embedDTO);
        if (rows <= 0) {
            throw new RuntimeException("插入嵌入关系失败");
        }

        return embedDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmbedCreateResponse createAndEmbed(Long docId, EmbedCreateRequest request) {
        // 1. 业务校验：检查被嵌入的文档是否存在
        DocumentDTO doc = documentService.getDocDetailById(docId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        } else if (!doc.getType().equals("doc")) {
            throw new BusinessException("嵌入的笔记只能是文档类型");
        }

        // 2. 业务校验：embedType 必填
        if (request.getEmbedType() == null || request.getEmbedType().isEmpty()) {
            throw new BusinessException("嵌入类型不能为空");
        }

        Long noteId = request.getNoteId();
        DocumentDTO createdNote = null;

        // 3. 如果没有提供noteId，则创建新的组件笔记
        if (noteId == null) {
            // 获取文档所在知识库ID
            Long kbId = doc.getKbId();
            
            // 校验用户对知识库的编辑权限
            if (!kbService.checkEditPermission(kbId)) {
                throw new BusinessException(403, "没有知识库编辑权限");
            }
            
            // 创建新组件笔记
            createdNote = createNewComponent(request, kbId);
            noteId = createdNote.getId();
        }

        // 4. 组装嵌入关系并入库
        EmbedDTO embedDTO = new EmbedDTO();
        embedDTO.setDocId(docId);
        embedDTO.setEmbedType(request.getEmbedType());
        embedDTO.setNoteId(noteId);
        embedDTO.setPosition(request.getPosition());
        embedDTO.setCreatedAt(LocalDateTime.now());

        int rows = embedMapper.insert(embedDTO);
        if (rows <= 0) {
            throw new RuntimeException("插入嵌入关系失败");
        }

        // 5. 构建响应
        EmbedCreateResponse response = new EmbedCreateResponse();
        response.setEmbedId(embedDTO.getEmbedId());
        response.setDocId(docId);
        response.setEmbedType(request.getEmbedType());
        response.setNoteId(noteId);
        response.setPosition(request.getPosition());
        response.setCreatedAt(embedDTO.getCreatedAt());

        // 如果是新建的笔记，返回完整笔记信息
        if (createdNote != null) {
            response.setNote(createdNote);
        }

        return response;
    }

    /**
     * 根据请求创建新的组件笔记
     * @param request 请求参数
     * @param kbId 知识库ID（新组件归属的知识库）
     */
    private DocumentDTO createNewComponent(EmbedCreateRequest request, Long kbId) {
        String embedType = request.getEmbedType();

        switch (embedType) {
            case "table":
                if (request.getNewTable() == null) {
                    throw new BusinessException("创建表格需要提供 newTable 参数");
                }
                TableDTO tableDTO = new TableDTO();
                tableDTO.setTitle(request.getNewTable().getTitle());
                tableDTO.setColumns(request.getNewTable().getColumns());
                tableDTO.setRows(request.getNewTable().getRows());
                tableDTO.setKbId(kbId);
                tableDTO.setParentDocId(null); // 不作为文档树的子节点
                return documentService.addTable(tableDTO);

            case "board":
                if (request.getNewBoard() == null) {
                    throw new BusinessException("创建画板需要提供 newBoard 参数");
                }
                BoardDTO boardDTO = new BoardDTO();
                boardDTO.setTitle(request.getNewBoard().getTitle());
                boardDTO.setElements(request.getNewBoard().getElements());
                boardDTO.setBackground(request.getNewBoard().getBackground());
                boardDTO.setKbId(kbId);
                boardDTO.setParentDocId(null); // 不作为文档树的子节点
                return documentService.addBoard(boardDTO);

            case "mind":
                if (request.getNewMind() == null) {
                    throw new BusinessException("创建思维导图需要提供 newMind 参数");
                }
                MindDTO mindDTO = new MindDTO();
                mindDTO.setTitle(request.getNewMind().getTitle());
                mindDTO.setNodes(request.getNewMind().getMindData()); // 使用 mindData 字段
                mindDTO.setKbId(kbId);
                mindDTO.setParentDocId(null); // 不作为文档树的子节点
                return documentService.addMind(mindDTO);

            default:
                throw new BusinessException("不支持的嵌入类型: " + embedType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmbed(Long docId, Long embedId) {
        // 1. 业务校验：通过embedId检查嵌套组件是否存在
        EmbedDTO embedDTO = embedMapper.getEmbedById(embedId);
        if (embedDTO == null) {
            throw new BusinessException("嵌套组件不存在");
        }
        // 2. 删除嵌入关系
        embedMapper.deleteEmbed(docId, embedId);
    }

    @Override
    public List<EmbedDTO> getEmbeds(Long docId, boolean includeData) {
        // 1. 业务校验：检查被嵌入的文档是否存在
        DocumentDTO doc = documentService.getDocDetailById(docId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        } else if (!doc.getType().equals("doc")) {
            throw new BusinessException("嵌入的笔记只能是文档类型");
        }

        // 2. 权限校验：用户必须对文档所在知识库拥有查看权限
        Long kbId = doc.getKbId();
        checkViewPermission(kbId);

        // 3. 查询嵌入关系
        List<EmbedDTO> embedList = embedMapper.getEmbeds(docId);

        // 4. 如果需要返回完整内容数据
        if (includeData && !embedList.isEmpty()) {
            for (EmbedDTO embed : embedList) {
                DocumentDTO note = documentService.getDocDetailById(embed.getNoteId());
                if (note != null && note.getContent() != null) {
                    try {
                        Map<String, Object> content = objectMapper.readValue(
                            note.getContent(),
                            new TypeReference<Map<String, Object>>() {}
                        );
                        embed.setContent(content);
                    } catch (Exception e) {
                        // 解析失败时忽略，不影响其他数据
                    }
                }
            }
        }

        return embedList;
    }

    /**
     * 校验用户对知识库的查看权限
     * 用户必须拥有 VIEWER/EDITOR/OWNER 角色之一
     * 管理员拥有所有权限
     */
    private void checkViewPermission(Long kbId) {
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException(403, "请先登录");
        }

        // 管理员直接放行
        com.ai.dto.UserDTO user = userMapper.getUserById(userId);
        if (user != null && "ADMIN".equals(user.getRole())) {
            return;
        }

        KBMemberDTO member = kbMemberMapper.selectByKbIdAndUserId(kbId, userId);
        if (member == null) {
            throw new BusinessException(403, "没有知识库查看权限");
        }

        String role = member.getRole();
        if (!"VIEWER".equals(role) && !"EDITOR".equals(role) && !"OWNER".equals(role)) {
            throw new BusinessException(403, "没有知识库查看权限");
        }
    }
}