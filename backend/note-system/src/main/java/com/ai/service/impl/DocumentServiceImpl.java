package com.ai.service.impl;


import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.ai.mapper.*;
import com.ai.pojo.PageResult;
import com.ai.utils.MarkdownToHtmlConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.element.*;
import com.itextpdf.styledxmlparser.jsoup.nodes.Element;
import com.itextpdf.styledxmlparser.jsoup.select.Elements;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.helper.W3CDom;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAltChunk;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.ai.dto.*;
import com.ai.exception.BusinessException;
import com.ai.service.DocumentService;
import com.ai.utils.CurrentHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private KBMapper kbMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private KbTableMapper kbTableMapper;
    @Autowired
    private KbBoardMapper kbBoardMapper;
    @Autowired
    private KbMindMapper kbMindMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserLikeMapper userLikeMapper;
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    @Autowired
    private KBMemberMapper kbMemberMapper;
    @Autowired
    private DocumentVersionMapper documentVersionMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ShareMapper shareMapper;
    @Autowired
    private UserBrowseHistoryMapper userBrowseHistoryMapper;
    @Autowired
    private CommentMapper commentMapper;

    @Value("${app.share.base-url:http://localhost:8080/api/public/note/}")
    private String shareBaseUrl;
    @Value("${github.token}")
    private String githubToken;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO addDoc(DocumentDTO documentDTO) {
        fillCommonFields(documentDTO, "doc");

        documentMapper.addDoc(documentDTO);

        // 4. 此时 documentDTO.getId() 已有值，可直接返回
        return documentDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableDTO addTable(TableDTO tableDTO) {
        // 1. 【第一步】先构建表格数据 JSON
        TableDTO tableData = new TableDTO(tableDTO.getColumns(), tableDTO.getRows());
        String tableDataJson;
        try {
            tableDataJson = objectMapper.writeValueAsString(tableData);
        } catch (JsonProcessingException e) {
            throw new BusinessException("表格数据序列化失败");
        }

        // 2. 【第二步】填充通用字段，并【关键】给父类的 content 赋值
        fillCommonFields(tableDTO, "table");

        // 因为 TableDTO 继承了 DocumentDTO，所以可以直接调用 setContent
        // 这里的 content 会存入 document 表
        tableDTO.setContent(tableDataJson);

        // 3. 【第三步】插入 Document
        // 此时 tableDTO 里既有 title，也有 content 了
        // 修改后的代码片段
        documentMapper.addDoc(tableDTO);


        // 【关键】确保这步执行完后，tableDTO.getId() 能取到值！
        // 这取决于 DocumentMapper.xml 的配置

        // 4. 保存到 kb_table 表
        KbTable kbTable = new KbTable();
        kbTable.setDocId(tableDTO.getId()); // 这里现在应该能拿到 ID 了
        kbTable.setKbId(tableDTO.getKbId());
        kbTable.setName(tableDTO.getTitle());
        kbTable.setCreateTime(LocalDateTime.now());
        kbTable.setTableData(tableDataJson); // 这里也存一份 JSON

        kbTableMapper.insert(kbTable);
        return tableDTO;
    }

    @Override
    public BoardDTO addBoard(BoardDTO boardDTO) {
        // 1. 参数校验
        validateBoardParams(boardDTO);

        // 2. 【修改点 1】提前构建画板扩展数据并序列化为 JSON
        BoardDTO board = new BoardDTO(boardDTO.getElements(), boardDTO.getBackground());
        String boardDataJson;
        try {
            boardDataJson = objectMapper.writeValueAsString(board);
        } catch (JsonProcessingException e) {
            throw new BusinessException("画板数据序列化失败");
        }

        // 3. 填充通用字段
        fillCommonFields(boardDTO, "board");

        // 4. 【修改点 2】关键！将 JSON 赋值给 content 字段（继承自 DocumentDTO）
        boardDTO.setContent(boardDataJson);

        // 5. 插入 document 表（此时 content 已经有值了）
        int rows = documentMapper.addDoc(boardDTO);
        if (rows <= 0) {
            throw new BusinessException("创建画板文档失败");
        }

        // 6. 保存到 kb_board 表
        KbBoard kbBoard = new KbBoard();
        kbBoard.setDocId(boardDTO.getId()); // 确保 DocumentMapper.xml 配置了主键回写
        kbBoard.setKbId(boardDTO.getKbId());
        kbBoard.setName(boardDTO.getTitle());
        kbBoard.setCreateTime(LocalDateTime.now());
        kbBoard.setBoardData(boardDataJson);
        kbBoardMapper.insert(kbBoard);

        return boardDTO;
    }

    @Override
    public MindDTO addMind(MindDTO mindDTO) {
        // 1. 参数校验
        validateMindParams(mindDTO);

        // 2. 【修改点 1】提前序列化 nodes 为 JSON 字符串
        String nodesJson;
        try {
            nodesJson = objectMapper.writeValueAsString(mindDTO.getNodes());
        } catch (JsonProcessingException e) {
            throw new BusinessException("思维导图节点数据序列化失败");
        }

        // 3. 填充通用字段（类型为 "mind"）
        fillCommonFields(mindDTO, "mind");

        // 4. 【修改点 2】关键！将 JSON 赋值给 content 字段（继承自 DocumentDTO）
        mindDTO.setContent(nodesJson);

        // 5. 插入 document 表（此时 content 已有值，且主键会自动回填）
        int rows = documentMapper.addDoc(mindDTO);
        if (rows <= 0) {
            throw new BusinessException("创建思维导图文档失败");
        }

        // 6. 插入 kb_mind 表
        KbMind kbMind = new KbMind();
        kbMind.setDocId(mindDTO.getId()); // 确保 DocumentMapper.xml 配置了主键回写
        kbMind.setKbId(mindDTO.getKbId());
        kbMind.setName(mindDTO.getTitle());
        kbMind.setCreatedAt(LocalDateTime.now());
        kbMind.setUpdatedAt(LocalDateTime.now());
        kbMind.setMindData(nodesJson);
        kbMindMapper.insert(kbMind);

        return mindDTO;
    }

    @Override
    public DocumentDTO getDocDetailById(Long docId) {
        // 1. 查询基础文档
        DocumentDTO baseDoc = documentMapper.getDocDetailById(docId);
        if (baseDoc == null) {
            throw new BusinessException("笔记不存在");
        }

        // 2. 检查是否在回收站（deleted_at 不为空）
        if (baseDoc.getDeletedAt() != null) {
            throw new BusinessException("笔记已在回收站，无法查看");
        }

        // 权限校验
        Long currentUserId = CurrentHolder.getCurrentId();
        // 1. 公开笔记直接放行（无需任何身份）
        if (baseDoc.getIsPublic() != null && baseDoc.getIsPublic() == 1) {
            // 可以增加审核状态校验（可选）
            // if (baseDoc.getAuditStatus() != 1) throw new BusinessException("笔记审核未通过");
            // 直接放行，不抛异常
        }
        // 2. 私有笔记必须登录
        else if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        // 2.5 管理员可以查看任意笔记
        else if (isAdmin(currentUserId)) {
            // 管理员直接放行
        }
        // 3. 私有笔记且已登录，检查细粒度权限（创建者/知识库成员）
        else if (!hasPermission(baseDoc, currentUserId)) {
            throw new BusinessException("无权查看该笔记");
        }


        // 3. 根据类型构建具体 DTO（填充扩展数据）
        DocumentDTO detail = buildSpecificDTO(baseDoc);

        // 4. 填充点赞/收藏状态（需获取当前登录用户）
        if (currentUserId != null) {
            // 查询点赞
            boolean hasLiked = userLikeMapper.existsByUserAndTarget(currentUserId, "DOC", docId);
            detail.setHasLiked(hasLiked);
            // 查询收藏
            boolean hasFavorited = userFavoriteMapper.existsByUserAndDoc(currentUserId, docId);
            detail.setHasFavorited(hasFavorited);
        }

        // 5. 异步增加浏览次数（推荐）
        incrementViewCountAsync(docId);

        return detail;
    }

    // 文件配置
    private static final String FILE_DOMAIN = "https://tos-cn-i-ik7evvg4ik.example.com/";
    private static final Pattern IMG_PATTERN = Pattern.compile("!\\[.*?]\\((.*?)\\)|<img.*?src=\"(.*?)\"");

    /**
     * 判断用户是否为管理员
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        UserDTO user = userMapper.getUserById(userId);
        return user != null && "ADMIN".equals(user.getRole());
    }

    /**
     * 判断用户对私有笔记是否有访问权限（调用前需确保笔记非公开且用户已登录）
     */
    private boolean hasPermission(DocumentDTO doc, Long userId) {
        // 1. 笔记创建者
        if (userId != null && doc.getCreatorId() != null && doc.getCreatorId().equals(userId)) {
            return true;
        }
        // 2. 知识库成员（不限角色）
        String role = kbMemberMapper.selectRoleByKbIdAndUserId(doc.getKbId(), userId);
        return role != null;
    }

    /**
     * 根据笔记类型构建对应的 DTO（填充 columns/rows 等扩展字段）
     */
    private DocumentDTO buildSpecificDTO(DocumentDTO base) {
        if (base == null || base.getType() == null) {
            return base;
        }

        DocumentDTO result = base;

        switch (base.getType()) {
            case "table":
                TableDTO tableDTO = new TableDTO();
                BeanUtils.copyProperties(base, tableDTO);
                KbTable tableExt = kbTableMapper.selectByDocId(base.getId());
                if (tableExt != null && StringUtils.hasText(tableExt.getTableData())) {
                    // 直接解析为 TableDTO
                    TableDTO extData = parseJson(tableExt.getTableData(), TableDTO.class);
                    tableDTO.setColumns(extData.getColumns());
                    tableDTO.setRows(extData.getRows());
                }
                result = tableDTO;
                break;

            case "board":
                BoardDTO boardDTO = new BoardDTO();
                BeanUtils.copyProperties(base, boardDTO);
                KbBoard boardExt = kbBoardMapper.selectByDocId(base.getId());
                if (boardExt != null && StringUtils.hasText(boardExt.getBoardData())) {
                    // ============== 修复点：直接解析为 BoardDTO，废弃 BoardData ==============
                    BoardDTO extData = parseJson(boardExt.getBoardData(), BoardDTO.class);
                    boardDTO.setElements(extData.getElements());
                    boardDTO.setBackground(extData.getBackground());
                }
                result = boardDTO;
                break;

            case "mind":
                MindDTO mindDTO = new MindDTO();
                BeanUtils.copyProperties(base, mindDTO);
                KbMind mindExt = kbMindMapper.selectByDocId(base.getId());
                if (mindExt != null && StringUtils.hasText(mindExt.getMindData())) {
                    List<MindDTO.Node> nodes = parseJson(mindExt.getMindData(),
                            new TypeReference<List<MindDTO.Node>>() {});
                    mindDTO.setNodes(nodes);
                }
                result = mindDTO;
                break;

            default:
                break;
        }

        return result;
    }

    // ========== 辅助方法 ==========
    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new BusinessException("扩展数据解析失败");
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (IOException e) {
            throw new BusinessException("扩展数据解析失败");
        }
    }

    @Async
    public void incrementViewCountAsync(Long docId) {
        try {
            if (docId == null) {
                log.warn("浏览量自增失败：笔记ID为空");
                return;
            }
            documentMapper.incrementViewCount(docId);
        } catch (Exception e) {
            log.error("浏览量自增失败，笔记ID: {}", docId, e);
        }
    }



    /**
     * 填充通用字段（所有笔记类型共用）
     * @param dto 文档DTO
     * @param type 笔记类型（doc/table/board/mind）
     */
    private void fillCommonFields(DocumentDTO dto, String type) {
        // 1. 参数校验（可共用）
        validateParams(dto);

        // 2. 设置类型
        dto.setType(type);

        // 3. 填充审计字段
        Long currentUserId = CurrentHolder.getCurrentId();
        dto.setCreatorId(currentUserId);
        dto.setLastEditorId(currentUserId);
        dto.setVersion(1);
        dto.setIsPublic(Objects.requireNonNullElse(dto.getIsPublic(), 0));
        dto.setViewCount(0);
        dto.setLikeCount(0);
        dto.setCommentCount(0);
        dto.setAuditStatus(0);
        dto.setDeletedAt(null);
    }
    private void validateParams(DocumentDTO dto) {
        if (dto.getKbId() == null) {
            throw new BusinessException("知识库ID不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
    }
    private void validateBoardParams(BoardDTO dto) {
        validateParams(dto);
        // 画板元素可为空，但背景色应有默认值
        if (!StringUtils.hasText(dto.getBackground())) {
            dto.setBackground("#ffffff");
        }
    }
    private void validateMindParams(MindDTO dto) {
        validateParams(dto);
        if (CollectionUtils.isEmpty(dto.getNodes())) {
            throw new BusinessException("思维导图节点不能为空");
        }
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO updateNote(Long noteId, Map<String, Object> body) {
        // 1. 获取并校验原笔记
        DocumentDTO origin = getAndValidateOrigin(noteId);

        // 【加这行】确认 origin 本身是否有 ID
        System.out.println("updateNote 拿到的 origin.id = " + origin.getId());


        // 2. 权限校验
        checkEditPermission(origin);
        // 3. 保存旧版本快照
        saveDocumentVersion(origin);
        // 4. 更新通用属性
        applyCommonUpdates(origin, body);
        // 5. 按类型更新扩展数据
        updateExtension(origin, body);
        // 6. 持久化主表
        documentMapper.updateById(origin);
        // 7. 返回完整详情
        return getDocDetailById(noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO moveDoc(Long noteId, Long targetKbId, Long targetParentDocId) {
        // 1. 校验原笔记
        DocumentDTO origin = getAndValidateOrigin(noteId);

        // 2. 校验目标知识库
         KBDTO targetKb = kbMapper.getKBDetailById(targetKbId);
         if (targetKb == null) throw new BusinessException(404, "目标知识库不存在");

        // 3. 权限校验（原笔记编辑权限 + 目标知识库编辑权限）
        checkEditPermission(origin); // 原笔记编辑权限
        checkTargetKbPermission(targetKbId); // 目标知识库编辑权限

        // 4. 保存旧版本快照（可选，和 updateNote 保持一致）
        saveDocumentVersion(origin);

        // 5. 更新主表字段
        origin.setKbId(targetKbId);
        origin.setParentDocId(targetParentDocId);
        origin.setVersion(origin.getVersion() + 1);
        origin.setLastEditorId(CurrentHolder.getCurrentId());
        origin.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(origin);

        // 6. 同步更新扩展表的 kb_id（如果扩展表有该字段）
        Map<String, Object> moveBody = new HashMap<>();
        moveBody.put("action", "move"); // 特殊标记：这是移动操作
        moveBody.put("newKbId", targetKbId); // 传递新的知识库ID
        updateExtension(origin, moveBody);

        // 7. 返回移动后的完整详情
        return getDocDetailById(noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(Long noteId) {
        // 1. 校验原笔记（复用现有方法，确保笔记存在且未被删除）
        DocumentDTO origin = getAndValidateOrigin(noteId);

        // 2. 双重校验：防止重复删除（如果已经在回收站，直接返回）
        if (origin.getDeletedAt() != null) {
            log.info("文档已在回收站，无需重复删除，noteId={}", noteId);
            return;
        }

        // 3. 权限校验（复用编辑权限，能编辑即可删除）
        checkEditPermission(origin);

        // 4. 保存旧版本快照（和更新/移动逻辑保持一致，留痕）
        saveDocumentVersion(origin);

        // 5. 主表软删除：仅设置 deleted_at，不碰扩展表
        origin.setDeletedAt(LocalDateTime.now()); // 核心：软删除标记
        origin.setVersion(origin.getVersion() + 1);
        origin.setLastEditorId(CurrentHolder.getCurrentId());
        origin.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(origin);

        // 6. 扩展表不做任何处理（按需求：无软删除字段，保持原样）
        log.info("文档软删除完成，noteId={}", noteId);
    }

    @Override
    public PageResult<RecycleDocDTO> getRecycleDocs(Integer page, Integer size) {
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 1. 【新增】先查一次当前用户的名字（只查一次，性能影响很小）
        String creatorName = userMapper.selectNameById(userId);
        // 兜底：防止查不到名字
        if (creatorName == null) {
            creatorName = "未知用户";
        }

        // 2. PageHelper 分页
        PageHelper.startPage(page, size);
        List<DocumentDTO> docDTOList = documentMapper.selectRecycleList(userId);
        PageInfo<DocumentDTO> pageInfo = new PageInfo<>(docDTOList);
        long total = pageInfo.getTotal();

        // 3. 转换 DTO
        String finalCreatorName = creatorName; // lambda 里要用 final 或 effectively final
        List<RecycleDocDTO> recycleList = docDTOList.stream().map(docDTO -> {
            RecycleDocDTO recycleDTO = new RecycleDocDTO();
            recycleDTO.setId(docDTO.getId());
            recycleDTO.setTitle(docDTO.getTitle());
            recycleDTO.setType(docDTO.getType());
            recycleDTO.setContent(docDTO.getContent() != null && docDTO.getContent().length() > 100
                    ? docDTO.getContent().substring(0, 100) + "..."
                    : docDTO.getContent());
            recycleDTO.setKbId(docDTO.getKbId());
            recycleDTO.setDeletedAt(docDTO.getDeletedAt());
            recycleDTO.setCreatorName(finalCreatorName); // 直接填充查好的名字
            return recycleDTO;
        }).collect(Collectors.toList());

        // 4. 组装返回
        PageResult<RecycleDocDTO> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setPage(page);
        pageResult.setSize(size);
        pageResult.setRecords(recycleList);
        return pageResult;
    }

    @Override
    public DocumentDTO restoreDoc(Long noteId) {
        // 1. 【关键】查询已删除的笔记（注意：要能查出 deleted_at 不为空的记录）
        DocumentDTO origin = getAndValidateDeletedOrigin(noteId);

        // 2. 防止重复恢复：如果已经不在回收站，直接返回
        if (origin.getDeletedAt() == null) {
            log.info("文档未删除，无需重复恢复，noteId={}", noteId);
            return getDocDetailById(noteId);
        }

        // 3. 权限校验（复用编辑权限，能删就能恢复）
        checkEditPermission(origin);

        // 4. 保存旧版本快照（和软删除保持一致，留痕）
        saveDocumentVersion(origin);

        // 5. 【核心反向操作】主表恢复：将 deleted_at 设为 null
        origin.setDeletedAt(null); // 恢复的核心
        origin.setVersion(origin.getVersion() + 1);
        origin.setLastEditorId(CurrentHolder.getCurrentId());
        origin.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(origin); // 复用更新方法

        // 6. 扩展表依然不做任何处理
        log.info("文档恢复完成，noteId={}", noteId);

        // 7. 返回恢复后的完整详情（符合接口文档要求）
        return getDocDetailById(noteId);
    }

    @Override
    public void deleteDocPermanently(Long noteId) {
        // 1. 【前置校验1】查询并校验：必须是“已软删除”的笔记（在回收站里）
        // 复用之前恢复逻辑里的辅助方法，确保 deleted_at 不为 null
        DocumentDTO origin = getAndValidateDeletedOrigin(noteId);

        // 2. 【前置校验2】权限校验（彻底删除通常权限更高，建议仅创建者/管理员可操作）
        // 这里复用编辑权限校验，也可单独实现 checkOwnerPermission(origin)
        checkEditPermission(origin);


        // 4. 【清理关联数据】删除历史版本快照（防止脏数据）
        documentVersionMapper.deleteByDocId(noteId);

        // 5. 【清理关联数据】删除扩展表数据（Mind/Board 等）
        // 保持与软删除逻辑的对称性，这里物理删除扩展表
        kbTableMapper.deleteByDocId(noteId);
        kbMindMapper.deleteByDocId(noteId);
        kbBoardMapper.deleteByDocId(noteId);

        // 6. 【核心操作】物理删除主表 Document 记录
        documentMapper.deleteById(noteId);
    }

    @Override
    public List<DocumentVersionDTO> getDocVersions(Long noteId) {
        // 1. 【前置校验】检查笔记是否存在，以及当前用户是否有查看权限
        getDocDetailById(noteId);

        // 2. 【核心查询】调用 Mapper 查询历史版本
        List<DocumentVersionDTO> versions = documentVersionMapper.listByDocId(noteId);

        return versions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO rollbackDoc(Long noteId, Long versionId) {
        // 1. 【前置校验1】检查当前笔记是否存在，以及用户是否有编辑权限
        DocumentDTO currentDoc = getDocDetailById(noteId);
        checkEditPermission(currentDoc);

        // 2. 【前置校验2】查询并校验目标历史版本
        // 必须确保：版本存在 + 该版本确实属于当前笔记
        DocumentVersionDTO targetVersion = documentVersionMapper.selectByIdAndDocId(versionId, noteId);
        if (targetVersion == null) {
            throw new BusinessException(404, "指定的历史版本不存在");
        }

        // 3. 【留痕】保存“回滚前”的当前状态到历史版本表
        // 把现在的样子存起来，防止后悔
        saveDocumentVersion(currentDoc);

        // 4. 【核心逻辑】用历史版本数据覆盖当前笔记
        // 只覆盖业务内容（title, content），不覆盖元数据（如创建时间、点赞数等）
        currentDoc.setTitle(targetVersion.getTitle());
        currentDoc.setContent(targetVersion.getContent());

        // 5. 更新元数据
        currentDoc.setVersion(currentDoc.getVersion() + 1); // 版本号+1（回滚是一次新的修改）
        currentDoc.setLastEditorId(CurrentHolder.getCurrentId());
        currentDoc.setUpdatedAt(LocalDateTime.now());

        // 6. 【同步扩展表】需同步回滚
        restoreExtTables(currentDoc, targetVersion);

        // 7. 执行更新
        documentMapper.updateById(currentDoc);


        // 8. 返回更新后的完整详情
        return getDocDetailById(noteId);
    }

    @Override
    public ShareDTO shareDoc(Long noteId, ShareDTO shareDTO) {
        // 1. 校验笔记存在性 & 基础权限
        DocumentDTO doc = getDocDetailById(noteId);

        // 2. 校验分享权限
        Long currentUserId = CurrentHolder.getCurrentId();
        if (!doc.getCreatorId().equals(currentUserId)) {
            String role = kbMemberMapper.selectRoleByKbIdAndUserId(doc.getKbId(), currentUserId);
            if (!"OWNER".equals(role)) {
                throw new BusinessException(403, "无分享权限");
            }
        }

        // 3. 校验权限类型合法性
        String permission = shareDTO.getPermission();
        if (!"READ".equals(permission) && !"EDIT".equals(permission)) {
            throw new BusinessException(400, "权限类型仅支持READ/EDIT");
        }

        // 4. 【核心调整】校验并处理分享时长
        String shareDuration = shareDTO.getShareDuration();
        if (!"HALF_YEAR".equals(shareDuration) && !"PERMANENT".equals(shareDuration)) {
            throw new BusinessException(400, "分享时长仅支持 HALF_YEAR（半年）或 PERMANENT（永久）");
        }

        LocalDateTime expireTime = null;
        if ("HALF_YEAR".equals(shareDuration)) {
            // 计算半年后的时间：当前时间 + 6个月
            expireTime = LocalDateTime.now().plusMonths(6);
        }
        // PERMANENT 时 expireTime 保持 null（永久）

        // 5. 生成唯一 shareKey
        String shareKey = generateUniqueShareKey();

        // 6. 处理密码（加密存储）
        String encryptedPassword = null;
        if (org.springframework.util.StringUtils.hasText(shareDTO.getPassword())) {
            encryptedPassword = passwordEncoder.encode(shareDTO.getPassword());
        }

        // 7. 组装 ShareDTO 并保存
        shareDTO.setDocId(noteId);
        shareDTO.setShareKey(shareKey);
        shareDTO.setPassword(encryptedPassword);
        shareDTO.setExpireTime(expireTime); // 【关键】设置计算后的过期时间
        shareDTO.setPermission(permission);
        shareDTO.setCreatorId(currentUserId);
        shareDTO.setCreatedAt(LocalDateTime.now());

        shareMapper.insert(shareDTO);

        // 8. 组装分享链接并返回
        String shareUrl = shareBaseUrl + shareKey;
        shareDTO.setShareUrl(shareUrl);

        return shareDTO;
    }

    @Override
    public ShareAccessDTO accessByShareKey(String shareKey, String password) {
        // 1. 查询分享记录
        ShareDTO shareDTO = shareMapper.selectByShareKey(shareKey);
        if (shareDTO == null) {
            throw new BusinessException(404, "分享链接不存在或已失效");
        }

        // 2. 检查分享是否过期
        LocalDateTime expireTime = shareDTO.getExpireTime();
        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "分享已过期");
        }

        // 3. 校验分享密码（若设置了密码）
        String encryptedPassword = shareDTO.getPassword();
        if (org.springframework.util.StringUtils.hasText(encryptedPassword)) {
            if (!org.springframework.util.StringUtils.hasText(password)) {
                throw new BusinessException(401, "请输入分享密码");
            }
            if (!passwordEncoder.matches(password, encryptedPassword)) {
                throw new BusinessException(401, "分享密码错误");
            }
        }

        // 4. 查询笔记（仅查未删除，无权限校验）
        Long docId = shareDTO.getDocId();
        DocumentDTO doc = documentMapper.selectDocByIdForShare(docId);
        if (doc == null) {
            throw new BusinessException(404, "笔记不存在或已删除");
        }

        // 5. 查询创建者名称
        String creatorName = userMapper.selectNameById(doc.getCreatorId());
        if (creatorName == null) {
            creatorName = "未知用户";
        }

        // 6. 组装返回数据
        ShareAccessDTO res = new ShareAccessDTO();
        res.setId(doc.getId());
        res.setTitle(doc.getTitle());
        res.setContent(doc.getContent());
        res.setType(doc.getType());
        res.setCreatorName(creatorName);
        res.setCreatedAt(doc.getCreatedAt());
        res.setPermission(shareDTO.getPermission());

        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Resource> exportNote(Long noteId, ExportFormatEnum formatEnum) {
        // 1. 复用已有方法获取笔记详情（内部已做权限校验，无权限会直接抛异常）
        DocumentDTO detail = getDocDetailById(noteId);

        // 2. 校验当前格式是否支持该笔记类型
        if (!formatEnum.getSupportTypes().contains(detail.getType())) {
            throw new BusinessException("该导出格式不支持此笔记类型");
        }

        // 3. 根据不同的枚举值生成文件数据
        ExportFile exportFile;
        switch (formatEnum) {
            case WORD:
                exportFile = generateWord(detail);
                break;
            case MARKDOWN:
                exportFile = generateMarkdown(detail);
                break;
            case PDF:
                exportFile = generatePdf(detail);
                break;
            case LAKE:
                exportFile = generateLake(detail);
                break;
            case JPG:
                exportFile = generateJpg(detail);
                break;
            case XLSX:
                exportFile = generateXlsx(detail);
                break;
            default:
                throw new BusinessException("不支持的导出格式");
        }

        // 4. 构建 ResponseEntity
        ByteArrayResource resource = new ByteArrayResource(exportFile.getData());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(exportFile.getContentType()));

        String fileName = detail.getTitle() + exportFile.getSuffix();
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordView(Long noteId) {
        UserBrowseHistoryDTO history = new UserBrowseHistoryDTO();
        history.setNoteId(noteId);
        history.setUserId(CurrentHolder.getCurrentId());
        history.setBrowseTime(LocalDateTime.now());
        userBrowseHistoryMapper.insert(history);
    }

    @Override
    public List<CommentDTO> getDocCommentsList(Long noteId) {
        // 一次性查询该笔记下的所有评论（不限层级）
        List<CommentDTO> allComments = commentMapper.selectAllCommentsByNoteId(noteId);
        return allComments;
    }

    @Override
    public DocumentDTO getVersionDetail(Long noteId, Long versionId) {
        // 1. 校验笔记存在，及权限
        getDocDetailById(noteId);

        // 2. 查询版本
        DocumentVersionDTO version = documentVersionMapper.selectByIdAndDocId(versionId, noteId);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }

        // 3. 将版本数据转为 DocumentDTO 返回（只包含展示所需字段）
        DocumentDTO dto = new DocumentDTO();
        dto.setId(noteId);
        dto.setTitle(version.getTitle());
        dto.setContent(version.getContent());
        dto.setType(getDocDetailById(noteId).getType());
        dto.setCreatorId(version.getEditorId());
        dto.setCreatedAt(version.getCreatedAt());
        // 不需要返回可编辑的元数据
        return dto;
    }


    /**
     * 封装导出文件的数据流、MIME 类型及文件后缀
     */
    @Data
    @AllArgsConstructor
    private static class ExportFile {
        private byte[] data;
        private String contentType;
        private String suffix;
    }

// ====================== 导出方法 ======================

    // Word
    private ExportFile generateWord(DocumentDTO detail) {
        String title = detail.getTitle();
        String content = detail.getContent();
        String markdown = (content != null) ? content : "";

        // 0. 处理嵌套组件 (生成 Markdown 语法，交给转换器渲染为 HTML)
        markdown = processEmbeds(markdown, false);

        // 1. Markdown → HTML
        String htmlBody = MarkdownToHtmlConverter.convert(markdown);

        // 2. 【关键】将远程图片转换为 Base64，Word 才能离线显示
        String embeddedHtml = embedRemoteImages(htmlBody);

        // 3. 构建完整的 HTML 文档（包含基本样式）
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Microsoft YaHei', sans-serif; margin: 50px; }");
        html.append("h1, h2, h3 { color: #333; }");
        html.append("table { border-collapse: collapse; width: 100%; }");
        html.append("th, td { border: 1px solid #999; padding: 8px; text-align: left; }");
        html.append("code { background-color: #f4f4f4; padding: 2px 6px; }");
        html.append("pre { background-color: #f4f4f4; padding: 15px; }");
        html.append("</style>");
        html.append("</head><body>");

        if (StringUtils.hasText(title)) {
            html.append("<h1>").append(escapeHtml(title)).append("</h1>");
        }
        html.append(embeddedHtml);   // 使用已嵌入图片的 HTML
        html.append("</body></html>");

        // 4. 返回为 .doc 文件（Word 可直接打开 HTML）
        byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
        return new ExportFile(bytes, "application/msword", ".doc");
    }


    // PDF
    private ExportFile generatePdf(DocumentDTO detail) {
        String title = detail.getTitle();
        String content = detail.getContent();
        String markdown = (content != null) ? content : "";

        // 0. 处理嵌套组件
        markdown = processEmbeds(markdown, false);

        // 1. Markdown → HTML 正文
        String htmlBody = MarkdownToHtmlConverter.convert(markdown);

        // 2. 【核心修复】将远程图片下载并转换为 Base64
        String embeddedHtml = embedRemoteImages(htmlBody);

        // 3. 构建完整的 HTML 文档
        String fullHtml = "<html><head><meta charset='UTF-8'/>"
                + "<style>"
                + "body { font-family: 'SimSun', '宋体', serif; margin: 50px; font-size: 12pt; }"
                + "h1, h2, h3 { font-family: 'SimHei', '黑体', sans-serif; }"
                + "table { border-collapse: collapse; width: 100%; }"
                + "th, td { border: 1px solid #ccc; padding: 8px; }"
                + "th { background-color: #f5f5f5; }"
                + "pre { background-color: #f5f5f5; padding: 10px; }"
                + "code { background-color: #f0f0f0; }"
                + "</style></head><body>"
                + (StringUtils.hasText(title) ? "<h1>" + escapeHtml(title) + "</h1>" : "")
                + embeddedHtml
                + "</body></html>";

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Document jsoupDoc = Jsoup.parse(fullHtml);
            jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            jsoupDoc.outputSettings().prettyPrint(false);

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withW3cDocument(new W3CDom().fromJsoup(jsoupDoc), "/");
            builder.toStream(os);

            // 添加中文字体（确保字体路径正确）
            builder.useFont(() -> getClass().getClassLoader().getResourceAsStream("fonts/simsun.ttc"), "SimSun");
            builder.useFont(() -> getClass().getClassLoader().getResourceAsStream("fonts/simhei.ttf"), "SimHei");

            builder.run();
            return new ExportFile(os.toByteArray(), "application/pdf", ".pdf");
        } catch (Exception e) {
            throw new BusinessException("PDF 生成失败: " + e.getMessage());
        }
    }

    /**
     * 网络图片转 Base64
     */
    private String embedRemoteImages(String htmlContent) {
        // 匹配 <img src="http...">
        Pattern imgPattern = Pattern.compile("<img\\s+[^>]*src=\"(https?://[^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = imgPattern.matcher(htmlContent);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String imgUrl = matcher.group(1);
            String base64 = convertImageToBase64(imgUrl);
            if (base64 != null) {
                String replacement = matcher.group(0).replace(imgUrl, "data:image/png;base64," + base64);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // 如果转换失败，保留原始图片链接（或替换为占位符）
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String convertImageToBase64(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream in = conn.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            System.err.println("无法下载图片转为Base64: " + imageUrl + " 错误: " + e.getMessage());
            return null;
        }
    }

    // ---------- Markdown 解析为 PDF 元素（不依赖 List/ListNumberingType）----------
    // 注意方法签名增加了 PdfFont 参数
    private List<com.itextpdf.layout.element.IBlockElement> parseMarkdownToPdfElements(String markdown, PdfFont font) {
        List<com.itextpdf.layout.element.IBlockElement> elements = new ArrayList<>();
        if (!StringUtils.hasText(markdown)) return elements;

        String[] lines = markdown.split("\\n");
        List<String> listBuffer = new ArrayList<>();
        boolean ordered = false;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                flushListBuffer(elements, listBuffer, ordered, font); // 传字体
                elements.add(new Paragraph("").setMarginBottom(2).setFont(font)); // 传字体
                continue;
            }

            // 标题
            if (line.matches("^#{1,6}\\s.*")) {
                flushListBuffer(elements, listBuffer, ordered, font);
                int level = line.indexOf(' ');
                String headingText = line.substring(level + 1);
                Paragraph p = buildFormattedParagraph(headingText, font) // 传字体
                        .setFontSize(20 - (level * 2))
                        .setBold()
                        .setMarginBottom(4);
                elements.add(p);
                continue;
            }

            // 引用
            if (line.startsWith("> ")) {
                flushListBuffer(elements, listBuffer, ordered, font);
                String quoteText = line.substring(2);
                Paragraph p = buildFormattedParagraph(quoteText, font) // 传字体
                        .setItalic()
                        .setMarginLeft(20)
                        .setFontColor(new DeviceRgb(100, 100, 100));
                elements.add(p);
                continue;
            }

            // 分割线
            if (line.matches("^[-*_]{3,}\\s*$")) {
                flushListBuffer(elements, listBuffer, ordered, font);
                elements.add(new LineSeparator(new SolidLine()));
                continue;
            }

            // 无序列表
            if (line.matches("^[-*]\\s.*")) {
                if (!listBuffer.isEmpty() && ordered) flushListBuffer(elements, listBuffer, ordered, font);
                ordered = false;
                listBuffer.add(line.replaceFirst("^[-*]\\s+", ""));
                continue;
            }

            // 有序列表
            if (line.matches("^\\d+\\.\\s.*")) {
                if (!listBuffer.isEmpty() && !ordered) flushListBuffer(elements, listBuffer, ordered, font);
                ordered = true;
                listBuffer.add(line.replaceFirst("^\\d+\\.\\s+", ""));
                continue;
            }

            // 普通段落
            flushListBuffer(elements, listBuffer, ordered, font);
            elements.add(buildFormattedParagraph(line, font)); // 传字体
        }
        flushListBuffer(elements, listBuffer, ordered, font);

        return elements;
    }

    // ---------- 列表缓冲刷新（用缩进段落模拟，完全兼容）----------
    private void flushListBuffer(List<com.itextpdf.layout.element.IBlockElement> elements,
                                 List<String> buffer, boolean ordered, PdfFont font) { // 增加字体参数
        if (buffer.isEmpty()) return;

        int counter = 1;
        for (String item : buffer) {
            String prefix = ordered? counter + ". " : "• ";
            counter++;
            Paragraph p = buildFormattedParagraph(prefix + item, font) // 传字体给构建器
                    .setMarginLeft(20)
                    .setFirstLineIndent(-10);
            elements.add(p);
        }
        buffer.clear();
    }

    // ---------- 构建支持粗斜体、代码的段落 ----------
    // 增加 PdfFont 参数
    private Paragraph buildFormattedParagraph(String text, PdfFont font) {
        Paragraph paragraph = new Paragraph().setFont(font);
        if (text == null) text = "";

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\*\\*(.*?)\\*\\*)|(\\*(.*?)\\*)|(`(.*?)`)");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                paragraph.add(new Text(text.substring(lastEnd, matcher.start())).setFont(font));
            }
            if (matcher.group(2)!= null) {
                paragraph.add(new Text(matcher.group(2)).setFont(font).setBold());
            }
            else if (matcher.group(4)!= null) {
                paragraph.add(new Text(matcher.group(4)).setFont(font).setItalic());
            }
            else if (matcher.group(6)!= null) {
                paragraph.add(new Text(matcher.group(6))
                        .setFont(font)
                        .setFontSize(10)
                        .setBackgroundColor(new DeviceRgb(240, 240, 240)));
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            paragraph.add(new Text(text.substring(lastEnd)).setFont(font));
        }

        return paragraph;
    }


    // JPG
    private ExportFile generateJpg(DocumentDTO detail) {
        String htmlContent;
        if ("doc".equals(detail.getType())) {
            String markdown = (detail.getContent() != null) ? detail.getContent() : "";
            // 0. 处理嵌套组件
            markdown = processEmbeds(markdown, false);
            String htmlBody = MarkdownToHtmlConverter.convert(markdown);
            // 改为本地化图片，不再用 Base64
            htmlBody = localizeImages(htmlBody);
            htmlContent = "<html><head><meta charset='UTF-8'/><style>" +
                    "body { font-family: 'Microsoft YaHei', sans-serif; margin: 20px; }" +
                    "img { max-width: 100%; }" +
                    "</style></head><body>" +
                    (StringUtils.hasText(detail.getTitle()) ? "<h1>" + escapeHtml(detail.getTitle()) + "</h1>" : "") +
                    htmlBody +
                    "</body></html>";
        } else if ("board".equals(detail.getType())) {
            htmlContent = buildBoardHtml(detail);
        } else {
            throw new BusinessException("当前笔记类型不支持导出为 JPG");
        }

        // 调用 wkhtmltoimage 截图
        try {
            Path htmlFile = Files.createTempFile("note_", ".html");
            Files.writeString(htmlFile, htmlContent);
            Path jpgFile = Files.createTempFile("snapshot_", ".jpg");

            String wkhtmlPath = "D:\\JavaWeb\\wkhtmltopdf\\bin\\wkhtmltoimage.exe";
            ProcessBuilder pb = new ProcessBuilder(
                    wkhtmlPath,
                    "--format", "jpg",
                    "--encoding", "UTF-8",
                    "--width", "800",
                    "--enable-local-file-access",
                    "--no-stop-slow-scripts",
                    htmlFile.toAbsolutePath().toString(),
                    jpgFile.toAbsolutePath().toString()
            );
            Process process = pb.start();
            process.getErrorStream().transferTo(System.out);
            process.getInputStream().transferTo(System.out);

            if (process.waitFor(15, TimeUnit.SECONDS) && process.exitValue() == 0) {
                byte[] bytes = Files.readAllBytes(jpgFile);
                // 清理临时文件
                Files.deleteIfExists(htmlFile);
                Files.deleteIfExists(jpgFile);
                // 同时清理图片临时目录（见 localizeImages 方法）
                Path imgDir = Paths.get(System.getProperty("java.io.tmpdir"), "note_images");
                if (Files.exists(imgDir)) {
                    Files.walk(imgDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(file -> {
                                try { Files.deleteIfExists(file); } catch (Exception ignored) {}
                            });
                }
                return new ExportFile(bytes, "image/jpeg", ".jpg");
            } else {
                throw new BusinessException("JPG生成失败，退出码：" + process.exitValue());
            }
        } catch (Exception e) {
            throw new BusinessException("JPG生成失败：" + e.getMessage());
        }
    }

    // 处理画板

    /**
     * 下载远程图片到临时目录，并将 HTML 中的 src 替换为本地路径
     */
    private String localizeImages(String htmlContent) {
        Pattern imgPattern = Pattern.compile(
                "<img\\s+[^>]*src=\"(https?://[^\"]+)\"[^>]*>",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = imgPattern.matcher(htmlContent);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String imgUrl = matcher.group(1);
            String localPath = downloadImageToTemp(imgUrl);
            if (localPath != null) {
                String replacement = matcher.group(0).replace(imgUrl, localPath);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // 下载失败，保留原始链接（或替换为占位符）
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String downloadImageToTemp(String imageUrl) {
        try {
            // 创建临时目录
            Path imgDir = Paths.get(System.getProperty("java.io.tmpdir"), "note_images");
            if (!Files.exists(imgDir)) {
                Files.createDirectories(imgDir);
            }

            // 生成临时文件名（保留扩展名）
            String extension = ".png";
            if (imageUrl.contains(".jpg") || imageUrl.contains(".jpeg")) {
                extension = ".jpg";
            }
            Path tempFile = Files.createTempFile(imgDir, "img_", extension);

            // 下载图片（增加超时和重试）
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 如果是 GitHub 链接，添加 Token 加速（可选）
            if (imageUrl.contains("raw.githubusercontent.com") && StringUtils.hasText(githubToken)) {
                conn.setRequestProperty("Authorization", "token " + githubToken);
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 返回本地文件绝对路径（需要符合 URI 格式）
            return tempFile.toUri().toString();
        } catch (Exception e) {
            System.err.println("下载图片失败: " + imageUrl + " 错误: " + e.getMessage());
            return null;
        }
    }

    private String buildBoardHtml(DocumentDTO detail) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder svg = new StringBuilder();

        int canvasWidth = 800;
        int canvasHeight = 600;
        String bgColor = "#ffffff";

        try {
            JsonNode root = mapper.readTree(detail.getContent());

            if (root.has("background") && root.get("background").isTextual()) {
                bgColor = root.get("background").asText();
            }

            svg.append("<svg width='").append(canvasWidth).append("' height='").append(canvasHeight)
                    .append("' xmlns='http://www.w3.org/2000/svg'>");
            svg.append("<rect width='100%' height='100%' fill='").append(bgColor).append("'/>");

            if (root.has("elements") && root.get("elements").isArray()) {
                for (JsonNode el : root.get("elements")) {
                    String type = el.has("type") ? el.get("type").asText() : "";
                    int x = el.has("x") ? el.get("x").asInt() : 0;
                    int y = el.has("y") ? el.get("y").asInt() : 0;
                    int w = el.has("width") ? el.get("width").asInt(100) : 100;
                    int h = el.has("height") ? el.get("height").asInt(50) : 50;
                    String content = el.has("content") ? el.get("content").asText("") : "";

                    // 通用颜色处理：优先用保存的 fill / stroke，否则用默认值
                    String fill = el.has("fill") && !el.get("fill").isNull() ? el.get("fill").asText() : "#cccccc";
                    String stroke = el.has("stroke") && !el.get("stroke").isNull() ? el.get("stroke").asText() : "#333333";
                    int strokeWidth = el.has("strokeWidth") ? el.get("strokeWidth").asInt(1) : 1;

                    if ("rect".equals(type)) {
                        svg.append("<rect x='").append(x).append("' y='").append(y)
                                .append("' width='").append(w).append("' height='").append(h)
                                .append("' fill='").append(fill).append("' stroke='").append(stroke)
                                .append("' stroke-width='").append(strokeWidth).append("'/>");
                        // 矩形内文字
                        if (!content.isEmpty()) {
                            svg.append("<text x='").append(x + w/2).append("' y='").append(y + h/2 + 5)
                                    .append("' text-anchor='middle' font-family='Arial' font-size='14' fill='#333'>")
                                    .append(escapeHtml(content)).append("</text>");
                        }
                    }

                    else if ("circle".equals(type)) {
                        int r = Math.max(w, h) / 2;
                        svg.append("<circle cx='").append(x + w/2).append("' cy='").append(y + h/2)
                                .append("' r='").append(r)
                                .append("' fill='").append(fill).append("' stroke='").append(stroke)
                                .append("' stroke-width='").append(strokeWidth).append("'/>");
                    }

                    else if ("line".equals(type)) {
                        int startX = el.has("startX") ? el.get("startX").asInt() : 0;
                        int startY = el.has("startY") ? el.get("startY").asInt() : 0;
                        int endX = el.has("endX") ? el.get("endX").asInt() : 0;
                        int endY = el.has("endY") ? el.get("endY").asInt() : 0;
                        svg.append("<line x1='").append(startX).append("' y1='").append(startY)
                                .append("' x2='").append(endX).append("' y2='").append(endY)
                                .append("' stroke='").append(stroke)
                                .append("' stroke-width='").append(strokeWidth).append("'/>");
                    }

                    else if ("i-text".equals(type)) {
                        String textContent = el.has("content") ? el.get("content").asText("") : "";
                        int fontSize = el.has("fontSize") ? el.get("fontSize").asInt(20) : 20;
                        String textFill = el.has("fill") && !el.get("fill").isNull() ? el.get("fill").asText() : "#000000";
                        svg.append("<text x='").append(x).append("' y='").append(y + fontSize)
                                .append("' font-family='Arial' font-size='").append(fontSize)
                                .append("' fill='").append(textFill).append("'>")
                                .append(escapeHtml(textContent)).append("</text>");
                    }

                    else if ("path".equals(type) && !content.isEmpty()) {
                        try {
                            JsonNode pathArray = mapper.readTree(content);
                            StringBuilder dAttr = new StringBuilder();
                            for (JsonNode cmdNode : pathArray) {
                                if (cmdNode.isArray()) {
                                    for (int i = 0; i < cmdNode.size(); i++) {
                                        if (i == 0) {
                                            dAttr.append(cmdNode.get(i).asText()).append(" ");
                                        } else {
                                            dAttr.append(cmdNode.get(i).asDouble()).append(" ");
                                        }
                                    }
                                }
                            }
                            svg.append("<path d='").append(dAttr.toString().trim())
                                    .append("' fill='none' stroke='").append(stroke)
                                    .append("' stroke-width='").append(strokeWidth).append("'/>");
                        } catch (Exception e) {
                            svg.append("<path d='M0,0 L100,100' fill='none' stroke='#000' stroke-width='2'/>");
                        }
                    }
                }
            }

            svg.append("</svg>");

        } catch (Exception e) {
            e.printStackTrace();
            svg.append("<div style='color:red;'>画板数据解析失败</div>");
        }

        return "<html><head><meta charset='UTF-8'/></head><body style='margin:0; padding:20px;'>" +
                "<h1 style='font-family:Arial;'>" + escapeHtml(detail.getTitle()) + "</h1>" +
                svg.toString() +
                "</body></html>";
    }

    // ---------- 嵌套组件处理 ----------

    /**
     * 处理文档内容中的嵌套组件占位符 {{embed|type|id}}
     */
    private String processEmbeds(String content, boolean isHtmlTarget) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        // 统一使用简单正则匹配占位符
        Pattern embedPattern = Pattern.compile("\\{\\{\\s*embed\\|(\\w+)\\|(\\d+)\\s*}}");
        Matcher matcher = embedPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String type = matcher.group(1);
            Long id = Long.valueOf(matcher.group(2));

            try {
                // 获取嵌套组件详情（内部已填充扩展数据）
                DocumentDTO detail = getDocDetailById(id);
                String replacement = "";

                if ("table".equals(type) && detail instanceof TableDTO) {
                    // 表格在 Markdown 阶段处理效果最好（生成 Word 原生表格）
                    replacement = isHtmlTarget ? convertTableToHtml((TableDTO) detail) : convertTableToMarkdown((TableDTO) detail);
                } else if ("mind".equals(type) && detail instanceof MindDTO) {
                    // 思维导图始终生成高质量 HTML，Flexmark 会透传 HTML 块
                    replacement = convertMindToHtml((MindDTO) detail);
                } else if ("board".equals(type) && detail instanceof BoardDTO) {
                    // 画板始终生成 SVG 嵌入
                    replacement = buildBoardHtmlEmbed((BoardDTO) detail);
                } else {
                    replacement = matcher.group(0);
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                log.warn("处理导出嵌套组件失败, id: {}, error: {}", id, e.getMessage());
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String convertTableToHtml(TableDTO table) {
        List<TableDTO.Column> columns = table.getColumns();
        List<TableDTO.Row> rows = table.getRows();
        if (CollectionUtils.isEmpty(columns)) return "";

        StringBuilder html = new StringBuilder("\n<table style='border-collapse: collapse; width: 100%; border: 1px solid #ddd;'>\n");
        // 表头
        html.append("  <tr style='background-color: #f2f2f2;'>\n");
        for (TableDTO.Column col : columns) {
            html.append("    <th style='border: 1px solid #ddd; padding: 8px; text-align: left;'>")
                .append(escapeHtml(col.getTitle())).append("</th>\n");
        }
        html.append("  </tr>\n");
        // 数据行
        if (!CollectionUtils.isEmpty(rows)) {
            for (TableDTO.Row row : rows) {
                html.append("  <tr>\n");
                Map<String, Object> cells = row.getCells();
                for (TableDTO.Column col : columns) {
                    Object val = cells != null ? cells.get(col.getKey()) : "";
                    html.append("    <td style='border: 1px solid #ddd; padding: 8px;'>")
                        .append(val != null ? escapeHtml(val.toString()) : "").append("</td>\n");
                }
                html.append("  </tr>\n");
            }
        }
        html.append("</table>\n");
        return html.toString();
    }

    private String convertMindToHtml(MindDTO mind) {
        List<MindDTO.Node> nodes = mind.getNodes();
        if (CollectionUtils.isEmpty(nodes)) return "";

        // 为 Word 导出生成高质量的 SVG 思维导图，以 Base64 图片嵌入
        String svg = buildMindSvg(mind);
        String base64Svg = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));

        return "\n<div style='margin: 20px 0; text-align: center;'>\n" +
               "  <div style='font-weight: bold; margin-bottom: 10px; color: #8c8c8c; font-size: 12px;'>思维导图: " + escapeHtml(mind.getTitle()) + "</div>\n" +
               "  <img src=\"data:image/svg+xml;base64," + base64Svg + "\" style='max-width: 100%; height: auto;' />\n" +
               "</div>\n";
    }

    /**
     * 构建思维导图 SVG (横向树状布局 + 贝塞尔曲线)
     */
    private String buildMindSvg(MindDTO mind) {
        List<MindDTO.Node> rootNodes = mind.getNodes();
        
        // 1. 计算布局参数
        int nodeWidth = 140;
        int nodeHeight = 40;
        int hGap = 60;
        int vGap = 20;
        
        // 计算每个节点的子树总高度
        Map<MindDTO.Node, Integer> subtreeHeights = new HashMap<>();
        for (MindDTO.Node root : rootNodes) {
            calculateSubtreeHeight(root, subtreeHeights, nodeHeight, vGap);
        }
        
        int totalHeight = rootNodes.stream().mapToInt(subtreeHeights::get).sum() + (rootNodes.size() - 1) * vGap;
        int totalWidth = calculateTreeDepth(rootNodes) * (nodeWidth + hGap);
        
        // 2. 绘制 SVG
        StringBuilder svg = new StringBuilder();
        svg.append("<svg width='").append(totalWidth + 40).append("' height='").append(totalHeight + 40)
           .append("' xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(totalWidth + 40).append(" ").append(totalHeight + 40).append("'>");
        svg.append("<rect width='100%' height='100%' fill='#ffffff'/>");
        
        int currentY = 20;
        String[] branchColors = {"#1890ff", "#722ed1", "#13c2c2", "#52c41a", "#eb2f96", "#fa8c16"};
        
        for (int i = 0; i < rootNodes.size(); i++) {
            MindDTO.Node root = rootNodes.get(i);
            renderMindNodeSvg(svg, root, 20, currentY, nodeWidth, nodeHeight, hGap, vGap, subtreeHeights, branchColors[i % branchColors.length], 0);
            currentY += subtreeHeights.get(root) + vGap;
        }
        
        svg.append("</svg>");
        return svg.toString();
    }

    private int calculateSubtreeHeight(MindDTO.Node node, Map<MindDTO.Node, Integer> heights, int nodeHeight, int vGap) {
        if (CollectionUtils.isEmpty(node.getChildren())) {
            heights.put(node, nodeHeight);
            return nodeHeight;
        }
        int childrenHeight = 0;
        for (MindDTO.Node child : node.getChildren()) {
            childrenHeight += calculateSubtreeHeight(child, heights, nodeHeight, vGap);
        }
        childrenHeight += (node.getChildren().size() - 1) * vGap;
        int h = Math.max(nodeHeight, childrenHeight);
        heights.put(node, h);
        return h;
    }

    private int calculateTreeDepth(List<MindDTO.Node> nodes) {
        if (CollectionUtils.isEmpty(nodes)) return 0;
        int maxDepth = 0;
        for (MindDTO.Node node : nodes) {
            maxDepth = Math.max(maxDepth, 1 + calculateTreeDepth(node.getChildren()));
        }
        return maxDepth;
    }

    private void renderMindNodeSvg(StringBuilder svg, MindDTO.Node node, int x, int y, int w, int h, int hGap, int vGap, 
                                  Map<MindDTO.Node, Integer> heights, String color, int level) {
        int nodeY = y + (heights.get(node) - h) / 2;
        
        // 1. 绘制连线 (贝塞尔曲线)
        if (!CollectionUtils.isEmpty(node.getChildren())) {
            int childX = x + w + hGap;
            int childYOffset = y;
            for (MindDTO.Node child : node.getChildren()) {
                int childNodeY = childYOffset + (heights.get(child) - h) / 2;
                
                // 绘制从当前节点右侧中点到子节点左侧中点的曲线
                int startX = x + w;
                int startY = nodeY + h / 2;
                int endX = childX;
                int endY = childNodeY + h / 2;
                
                svg.append("<path d='M ").append(startX).append(" ").append(startY)
                   .append(" C ").append(startX + hGap/2).append(" ").append(startY)
                   .append(", ").append(endX - hGap/2).append(" ").append(endY)
                   .append(", ").append(endX).append(" ").append(endY)
                   .append("' fill='none' stroke='").append(color).append("' stroke-width='2'/>");
                
                renderMindNodeSvg(svg, child, childX, childYOffset, w, h, hGap, vGap, heights, color, level + 1);
                childYOffset += heights.get(child) + vGap;
            }
        }
        
        // 2. 绘制节点矩形
        String bgColor = level == 0 ? color : (level == 1 ? "#e6f7ff" : "#ffffff");
        String textColor = level == 0 ? "#ffffff" : "#333333";
        String strokeColor = level == 0 ? "none" : color;
        
        svg.append("<rect x='").append(x).append("' y='").append(nodeY)
           .append("' width='").append(w).append("' height='").append(h)
           .append("' rx='4' ry='4' fill='").append(bgColor)
           .append("' stroke='").append(strokeColor).append("' stroke-width='1'/>");
        
        // 3. 绘制文字
        svg.append("<text x='").append(x + w / 2).append("' y='").append(nodeY + h / 2 + 5)
           .append("' text-anchor='middle' font-family='Arial' font-size='12' fill='").append(textColor).append("'>")
           .append(escapeHtml(node.getTitle())).append("</text>");
    }

    private String buildBoardHtmlEmbed(BoardDTO board) {
        String svg = buildBoardSvgOnly(board);
        // Word 对 raw <svg> 支持极差，转换为 Base64 Data URI 的 <img> 标签兼容性更好
        String base64Svg = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        
        return "\n<div style='margin: 20px 0; text-align: center;'>\n" +
               "  <div style='font-weight: bold; margin-bottom: 10px; color: #666;'>画板: " + escapeHtml(board.getTitle()) + "</div>\n" +
               "  <img src=\"data:image/svg+xml;base64," + base64Svg + "\" style='max-width: 100%; border: 1px solid #eee; padding: 5px;' />\n" +
               "</div>\n";
    }

    private String buildBoardSvgOnly(BoardDTO detail) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder svg = new StringBuilder();

        int canvasWidth = 800;
        int canvasHeight = 600;
        String bgColor = "#ffffff";

        try {
            // 注意：boardDTO 的数据可能在 content 中，也可能在 elements/background 中
            // 这里假设 detail 已经填充了 elements 和 background
            bgColor = detail.getBackground() != null ? detail.getBackground() : "#ffffff";

            svg.append("<svg width='").append(canvasWidth).append("' height='").append(canvasHeight)
                    .append("' viewBox='0 0 ").append(canvasWidth).append(" ").append(canvasHeight)
                    .append("' xmlns='http://www.w3.org/2000/svg' style='max-width: 100%; height: auto;'>");
            svg.append("<rect width='100%' height='100%' fill='").append(bgColor).append("'/>");

            if (!CollectionUtils.isEmpty(detail.getElements())) {
                for (BoardDTO.Element el : detail.getElements()) {
                    String type = el.getType();
                    // 兼容 x/y 和 left/top
                    int posX = el.getX() != null ? el.getX() : (el.getLeft() != null ? el.getLeft() : 0);
                    int posY = el.getY() != null ? el.getY() : (el.getTop() != null ? el.getTop() : 0);
                    
                    int w = el.getWidth() != null ? el.getWidth() : 100;
                    int h = el.getHeight() != null ? el.getHeight() : 50;
                    String content = el.getContent();

                    String fill = el.getFill() != null ? el.getFill() : "#cccccc";
                    String stroke = el.getStroke() != null ? el.getStroke() : "#333333";
                    int strokeWidth = el.getStrokeWidth() != null ? el.getStrokeWidth() : 1;

                    if ("rect".equals(type)) {
                        svg.append("<rect x='").append(posX).append("' y='").append(posY)
                                .append("' width='").append(w).append("' height='").append(h)
                                .append("' fill='").append(fill).append("' stroke='").append(stroke)
                                .append("' stroke-width='").append(strokeWidth).append("'/>");
                        if (StringUtils.hasText(content)) {
                            svg.append("<text x='").append(posX + w/2).append("' y='").append(posY + h/2 + 5)
                                    .append("' text-anchor='middle' font-family='Arial' font-size='14' fill='#333'>")
                                    .append(escapeHtml(content)).append("</text>");
                        }
                    }
                    else if ("circle".equals(type)) {
                        int r = Math.max(w, h) / 2;
                        svg.append("<circle cx='").append(posX + w/2).append("' cy='").append(posY + h/2)
                                .append("' r='").append(r)
                                .append("' fill='").append(fill).append("' stroke='").append(stroke)
                                .append("' stroke-width='").append(strokeWidth).append("'/>");
                    }
                    else if ("path".equals(type) && StringUtils.hasText(content)) {
                        try {
                            JsonNode pathArray = mapper.readTree(content);
                            StringBuilder dAttr = new StringBuilder();
                            for (JsonNode cmdNode : pathArray) {
                                if (cmdNode.isArray()) {
                                    for (int i = 0; i < cmdNode.size(); i++) {
                                        if (i == 0) dAttr.append(cmdNode.get(i).asText()).append(" ");
                                        else dAttr.append(cmdNode.get(i).asDouble()).append(" ");
                                    }
                                }
                            }
                            svg.append("<path d='").append(dAttr.toString().trim())
                                    .append("' fill='none' stroke='").append(stroke)
                                    .append("' stroke-width='").append(strokeWidth).append("'/>");
                        } catch (Exception ignored) {}
                    }
                    else if ("line".equals(type)) {
                        int x1 = el.getStartX() != null ? el.getStartX() : 0;
                        int y1 = el.getStartY() != null ? el.getStartY() : 0;
                        int x2 = el.getEndX() != null ? el.getEndX() : 0;
                        int y2 = el.getEndY() != null ? el.getEndY() : 0;
                        svg.append("<line x1='").append(x1).append("' y1='").append(y1)
                                .append("' x2='").append(x2).append("' y2='").append(y2)
                                .append("' stroke='").append(stroke).append("' stroke-width='").append(strokeWidth).append("'/>");
                    }
                    else if ("text".equals(type) || "i-text".equals(type) || "textbox".equals(type)) {
                        String textVal = StringUtils.hasText(el.getText()) ? el.getText() : el.getContent();
                        if (StringUtils.hasText(textVal)) {
                            // 1. 文字颜色：默认黑色
                            String textFill = el.getFill() != null ? el.getFill() : "#000000";
                            
                            // 2. 字体大小处理
                            String fSize = StringUtils.hasText(el.getFontSize()) ? el.getFontSize() : "20";
                            int fontSizeInt = 20;
                            try {
                                String numericPart = fSize.replaceAll("[^0-9]", "");
                                if (!numericPart.isEmpty()) fontSizeInt = Integer.parseInt(numericPart);
                            } catch (Exception ignored) {}
                            if (fSize.matches("\\d+")) fSize += "px";
                            
                            String fFamily = StringUtils.hasText(el.getFontFamily()) ? el.getFontFamily() : "Arial";
                            
                            // 3. 坐标修正：将 Fabric.js 的 top 对齐到 SVG 的 baseline
                            int adjustedY = posY + (int)(fontSizeInt * 0.85);

                            svg.append("<text x='").append(posX).append("' y='").append(adjustedY)
                                    .append("' font-family='").append(fFamily).append("' font-size='").append(fSize)
                                    .append("' fill='").append(textFill).append("'>")
                                    .append(escapeHtml(textVal)).append("</text>");
                        }
                    }
                }
            }
            svg.append("</svg>");
        } catch (Exception e) {
            svg.append("<text x='10' y='30' fill='red'>画板解析失败</text>");
        }
        return svg.toString();
    }

    private String convertTableToMarkdown(TableDTO table) {
        List<TableDTO.Column> columns = table.getColumns();
        List<TableDTO.Row> rows = table.getRows();
        if (CollectionUtils.isEmpty(columns)) return "";

        StringBuilder md = new StringBuilder("\n\n");
        // 表头
        md.append("| ").append(columns.stream()
                .map(c -> StringUtils.hasText(c.getTitle()) ? c.getTitle() : " ")
                .collect(Collectors.joining(" | "))).append(" |\n");
        // 分隔线
        md.append("| ").append(columns.stream().map(c -> "---").collect(Collectors.joining(" | "))).append(" |\n");
        // 数据行
        if (!CollectionUtils.isEmpty(rows)) {
            for (TableDTO.Row row : rows) {
                Map<String, Object> cells = row.getCells();
                md.append("| ");
                for (TableDTO.Column col : columns) {
                    Object val = cells != null ? cells.get(col.getKey()) : "";
                    md.append(val != null ? escapeHtml(val.toString()) : " ").append(" | ");
                }
                md.append("\n");
            }
        }
        return md.toString() + "\n\n";
    }

    private String convertMindToMarkdown(MindDTO mind) {
        List<MindDTO.Node> nodes = mind.getNodes();
        if (CollectionUtils.isEmpty(nodes)) return "";

        StringBuilder md = new StringBuilder("\n\n");
        for (MindDTO.Node node : nodes) {
            appendMindNodeToMarkdown(md, node, 0);
        }
        return md.toString() + "\n\n";
    }

    private void appendMindNodeToMarkdown(StringBuilder md, MindDTO.Node node, int level) {
        if (node == null) return;
        md.append("  ".repeat(level)).append("- ").append(escapeHtml(node.getTitle())).append("\n");
        if (!CollectionUtils.isEmpty(node.getChildren())) {
            for (MindDTO.Node child : node.getChildren()) {
                appendMindNodeToMarkdown(md, child, level + 1);
            }
        }
    }

    // 工具方法：防止 XSS 并转义 HTML 特殊字符
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    // Markdown
    private ExportFile generateMarkdown(DocumentDTO detail) {
        String title = detail.getTitle();
        String content = detail.getContent();
        if (!StringUtils.hasText(content)) {
            content = "";
        }

        // 0. 处理嵌套组件
        content = processEmbeds(content, false);

        // 将 title 作为一级标题添加到最前面
        String fullMarkdown = "# " + (title != null ? title : "") + "\n\n" + content;

        // 1. 查找所有本地图片引用（基于定义好的 IMG_PATTERN 与 FILE_DOMAIN）
        Matcher matcher = IMG_PATTERN.matcher(fullMarkdown);
        Map<String, String> imageUrlToLocal = new LinkedHashMap<>();  // 原始URL -> 本地文件名
        int imgIndex = 0;
        while (matcher.find()) {
            String url = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (url != null && url.startsWith(FILE_DOMAIN)) {   // 只处理本地图片，外部链接保持不变
                String ext = url.contains(".") ? url.substring(url.lastIndexOf(".")) : ".png";
                String localName = "image_" + (++imgIndex) + ext;
                imageUrlToLocal.put(url, localName);
            }
        }

        // 2. 无图片 → 直接返回 .md 文本
        if (imageUrlToLocal.isEmpty()) {
            return new ExportFile(fullMarkdown.getBytes(StandardCharsets.UTF_8),
                    "text/markdown", ".md");
        }

        // 3. 有图片 → 打包为 ZIP
        return generateMarkdownZip(fullMarkdown, imageUrlToLocal);
    }

    private ExportFile generateMarkdownZip(String mdContent,
                                           Map<String, String> imageUrlToLocal) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 使用 Java NIO 创建安全临时目录 —— 兼容所有 Hutool 版本
        java.nio.file.Path tmpDirPath = null;
        try {
            tmpDirPath = java.nio.file.Files.createTempDirectory("markdown_export_");
            java.io.File tmpDir = tmpDirPath.toFile(); // 转为传统 File 对象

            // 1. 替换图片链接为相对路径（位于 images/ 子目录）
            String mdWithLocalImgs = mdContent;
            for (Map.Entry<String, String> entry : imageUrlToLocal.entrySet()) {
                mdWithLocalImgs = mdWithLocalImgs.replace(entry.getKey(), "images/" + entry.getValue());
            }

            // 2. 写入 md 文件
            java.io.File mdFile = new java.io.File(tmpDir, "note.md");
            cn.hutool.core.io.FileUtil.writeBytes(mdWithLocalImgs.getBytes(StandardCharsets.UTF_8), mdFile);

            // 3. 下载图片并保存到 images/ 子目录
            java.io.File imagesDir = new java.io.File(tmpDir, "images");
            if (!imagesDir.mkdir()) throw new BusinessException("创建图片临时目录失败");
            for (Map.Entry<String, String> entry : imageUrlToLocal.entrySet()) {
                String imgUrl = entry.getKey();
                String localName = entry.getValue();
                try {
                    byte[] imgBytes = HttpUtil.downloadBytes(imgUrl);
                    cn.hutool.core.io.FileUtil.writeBytes(imgBytes, new java.io.File(imagesDir, localName));
                } catch (Exception e) {
                    log.warn("下载图片失败，跳过: {}", imgUrl, e);
                }
            }

            // 4. 打包成 ZIP（依赖 Hutool）
            java.io.File zipFile = ZipUtil.zip(tmpDir);
            byte[] zipBytes = cn.hutool.core.io.FileUtil.readBytes(zipFile);
            return new ExportFile(zipBytes, "application/zip", ".zip");
        } catch (IOException e) {
            throw new BusinessException("生成 Markdown 压缩包失败");
        } finally {
            // 清理临时目录（Hutool 的 del 支持递归删除）
            if (tmpDirPath != null) {
                cn.hutool.core.io.FileUtil.del(tmpDirPath.toFile());
            }
        }
    }

    // Lake
    private ExportFile generateLake(DocumentDTO detail) {
        // 直接序列化完整 DTO（已包含扩展字段，如 table 的 columns/rows，board 的 elements/background）
        try {
            String lakeJson = objectMapper.writeValueAsString(detail);
            return new ExportFile(lakeJson.getBytes(StandardCharsets.UTF_8),
                    "application/octet-stream", ".lake");
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lake 导出序列化失败");
        }
    }
    // XLSX
    private ExportFile generateXlsx(DocumentDTO detail) {
        if (!"table".equals(detail.getType())) {
            throw new BusinessException("仅表格笔记支持导出为 Excel");
        }
        TableDTO tableDTO;
        if (detail instanceof TableDTO) {
            tableDTO = (TableDTO) detail;
        } else {
            throw new BusinessException("表格数据异常");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelWriter writer = ExcelUtil.getWriter(true); // true 表示写入 .xlsx

            List<TableDTO.Column> columns = tableDTO.getColumns();

            // 1. 设置列宽
            for (int i = 0; i < columns.size(); i++) {
                writer.setColumnWidth(i, 20);
            }

            // 2. 写入表头行
            List<String> headers = columns.stream()
                    .map(TableDTO.Column::getTitle)
                    .collect(Collectors.toList());
            writer.writeHeadRow(headers);

            // 3. 写入数据行
            List<TableDTO.Row> rows = tableDTO.getRows();
            if (!CollectionUtils.isEmpty(rows)) {
                for (TableDTO.Row row : rows) {
                    Map<String, Object> cellMap = row.getCells();
                    List<Object> rowValues = new ArrayList<>();
                    for (TableDTO.Column col : columns) {
                        String key = col.getKey();
                        rowValues.add(key != null ? cellMap.getOrDefault(key, "") : "");
                    }
                    writer.writeRow(rowValues, false); // false 表示非表头
                }
            }

            writer.flush(bos);
            return new ExportFile(bos.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    ".xlsx");
        } catch (IOException e) {
            throw new BusinessException("生成 Excel 失败");
        }
    }



    // ---------- 私有辅助方法 ----------

    /**
     * 获取笔记并验证状态
     */
    private DocumentDTO getAndValidateOrigin(Long noteId) {
        DocumentDTO origin = documentMapper.getDocDetailById(noteId);
        if (origin == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (origin.getDeletedAt() != null) {
            throw new BusinessException(400, "回收站笔记不可编辑，请先恢复");
        }
        return origin;
    }
    /**
     * 查询并校验已删除的笔记（必须能查出 deleted_at 不为空的记录）
     */
    private DocumentDTO getAndValidateDeletedOrigin(Long noteId) {
        // 调用 Mapper 层一个“不限制 deleted_at”的查询方法
        DocumentDTO doc = documentMapper.selectByIdIncludeDeleted(noteId);
        if (doc == null) {
            throw new BusinessException(404, "笔记不存在或已彻底删除");
        }
        return doc;
    }
    /**
     * 编辑权限检查：创建者 或 知识库 EDITOR/OWNER
     */
    private void checkEditPermission(DocumentDTO doc) {
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException(403, "请先登录");
        }
        if (doc.getCreatorId() != null && doc.getCreatorId().equals(userId)) {
            return;
        }
        String role = kbMemberMapper.selectRoleByKbIdAndUserId(doc.getKbId(), userId);
        if (!"EDITOR".equals(role) && !"OWNER".equals(role)) {
            throw new BusinessException(403, "没有编辑权限");
        }
    }
    /**
     * 校验目标知识库权限
     */
    private void checkTargetKbPermission(Long targetKbId) {
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException(403, "请先登录");
        }
        // 检查是否是目标知识库的成员（EDITOR/OWNER）
        String role = kbMemberMapper.selectRoleByKbIdAndUserId(targetKbId, userId);
        if (!"EDITOR".equals(role) && !"OWNER".equals(role)) {
            throw new BusinessException(403, "没有目标知识库的编辑权限");
        }
    }
    /**
     * 保存旧版本快照
     */
    private void saveDocumentVersion(DocumentDTO doc) {
        DocumentVersionDTO ver = new DocumentVersionDTO();
        ver.setDocId(doc.getId());
        ver.setVersion(doc.getVersion());
        ver.setTitle(doc.getTitle());
        ver.setContent(doc.getContent() != null ? doc.getContent() : "");
        ver.setEditorId(doc.getLastEditorId());
        documentVersionMapper.insert(ver);
    }
    /**
     * 恢复扩展表
     */
    private void restoreExtTables(DocumentDTO currentDoc, DocumentVersionDTO targetVersion) {
        // 1. 空值校验
        if (currentDoc == null || targetVersion == null || StringUtils.isEmpty(targetVersion.getContent())) {
            log.warn("回滚扩展表失败：参数为空，docId={}", currentDoc == null ? null : currentDoc.getId());
            return;
        }

        Long docId = currentDoc.getId();
        String docType = currentDoc.getType();
        Long kbId = currentDoc.getKbId();
        String snapshotJson = targetVersion.getContent(); // 直接用历史版本的content快照

        // 2. 按类型更新扩展表（核心：复用快照JSON，无需解析）
        switch (docType) {
            case "table":
                KbTable tableExt = new KbTable();
                tableExt.setDocId(docId);
                tableExt.setKbId(kbId);
                tableExt.setName(targetVersion.getTitle()); // 同步回滚标题
                tableExt.setTableData(snapshotJson);       // 直接用历史快照覆盖
                kbTableMapper.updateByDocId(tableExt);
                break;

            case "board":
                KbBoard boardExt = new KbBoard();
                boardExt.setDocId(docId);
                boardExt.setKbId(kbId);
                boardExt.setName(targetVersion.getTitle());
                boardExt.setBoardData(snapshotJson);       // 直接用历史快照覆盖
                kbBoardMapper.updateByDocId(boardExt);
                break;

            case "mind":
                KbMind mindExt = new KbMind();
                mindExt.setDocId(docId);
                mindExt.setKbId(kbId);
                mindExt.setName(targetVersion.getTitle());
                mindExt.setMindData(snapshotJson);         // 直接用历史快照覆盖
                kbMindMapper.updateByDocId(mindExt);
                break;

            case "doc":
                // 纯文档无扩展表，无需处理
                break;

            default:
                log.warn("不支持的回滚类型：{}，docId={}", docType, docId);
                throw new BusinessException("不支持的笔记类型回滚");
        }
    }

    /**
     * 应用通用字段更新（标题等），并强制保护不可变字段
     */
    private void applyCommonUpdates(DocumentDTO origin, Map<String, Object> body) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            mapper.updateValue(origin, body);
        } catch (JsonMappingException e) {
            throw new BusinessException(400, "请求字段类型错误：" + e.getOriginalMessage());
        } catch (IOException e) {
            throw new BusinessException("数据合并失败");
        }
        // 强制覆盖不可修改字段
        origin.setType(origin.getType()); // 原类型不变
        origin.setKbId(origin.getKbId());
        origin.setParentDocId(origin.getParentDocId());
        origin.setCreatorId(origin.getCreatorId());
        origin.setDeletedAt(null);          // 保持非回收站状态
        origin.setVersion(origin.getVersion() + 1);
        origin.setLastEditorId(CurrentHolder.getCurrentId());
        origin.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 根据笔记类型更新对应的扩展表
     */
    private void updateExtension(DocumentDTO origin, Map<String, Object> body) {

        System.out.println("updateExtension 进入，文档ID=" + origin.getId() + "，文档类型=" + origin.getType() + "，body内容=" + body);

        // 移动操作专属逻辑
        if ("move".equals(body.get("action"))) {
            System.out.println("【移动操作】识别到move标记，开始处理");
            Long newKbId = Long.valueOf(body.get("newKbId").toString());
            System.out.println("【移动操作】目标kbId=" + newKbId);

            switch (origin.getType()) {
                case "table":
                    System.out.println("【移动操作】处理table类型扩展表");
                    KbTable tableExt = new KbTable();
                    tableExt.setDocId(origin.getId());
                    tableExt.setKbId(newKbId);
                    kbTableMapper.updateKbIdByDocId(tableExt);
                    System.out.println("【移动操作】table扩展表更新完成");
                    break;
                case "board":
                    System.out.println("【移动操作】处理board类型扩展表");
                    KbBoard boardExt = new KbBoard();
                    boardExt.setDocId(origin.getId());
                    boardExt.setKbId(newKbId);
                    kbBoardMapper.updateKbIdByDocId(boardExt);
                    System.out.println("【移动操作】board扩展表更新完成");
                    break;
                case "mind":
                    System.out.println("【移动操作】处理mind类型扩展表，docId=" + origin.getId() + "，newKbId=" + newKbId);
                    KbMind mindExt = new KbMind();
                    mindExt.setDocId(origin.getId());
                    mindExt.setKbId(newKbId);
                    kbMindMapper.updateKbIdByDocId(mindExt);
                    break;
                default:
                    System.out.println("【移动操作】无扩展表，跳过");
                    break;
            }
            return;
        }

        // 原有内容更新逻辑，保持不变
        System.out.println("【内容更新】走原有内容更新逻辑");
        switch (origin.getType()) {
            case "table":
                updateTableExtension(origin, body);
                break;
            case "board":
                updateBoardExtension(origin, body);
                break;
            case "mind":
                updateMindExtension(origin, body);
                break;
            case "doc":
                break;
            default:
                throw new BusinessException(400, "不支持的笔记类型");
        }
    }

    // ---------- 扩展表更新方法 ----------

    private void updateTableExtension(DocumentDTO origin, Map<String, Object> body) {
        // 【加这行】最后一道关卡
        System.out.println("updateTableExtension 收到的 docId = " + origin.getId());
        // 仅当传递了columns或rows时才更新
        if (!body.containsKey("columns") && !body.containsKey("rows")) return;
        KbTable ext = kbTableMapper.selectByDocId(origin.getId());
        System.out.println("查询结果 ext = " + ext); // 【加这行】
        if (ext == null) throw new BusinessException("表格扩展数据不存在");
        TableDTO temp = parseTableData(ext.getTableData());

        boolean hasDataChange = false;
        if (body.containsKey("columns")) {
            temp.setColumns(convertValue(body.get("columns"), new TypeReference<List<TableDTO.Column>>() {}));
            hasDataChange = true;
        }
        if (body.containsKey("rows")) {
            temp.setRows(convertValue(body.get("rows"), new TypeReference<List<TableDTO.Row>>() {}));
            hasDataChange = true;
        }

        // 同步修复1：更新标题到kb_table.name
        ext.setName(origin.getTitle());
        // 同步修复2：回写最新数据到document.content
        if (hasDataChange) {
            String newExtJson = toJson(new TableDTO(temp.getColumns(), temp.getRows()));
            ext.setTableData(newExtJson);
            origin.setContent(newExtJson);
        }

        kbTableMapper.updateByDocId(ext);
    }

    private void updateBoardExtension(DocumentDTO origin, Map<String, Object> body) {
        Long docId = origin.getId();
        // 1. 查询扩展表数据
        KbBoard ext = kbBoardMapper.selectByDocId(docId);
        if (ext == null) throw new BusinessException("画板扩展数据不存在");

        // 2. 合并新的扩展字段
        BoardDTO temp = parseBoardData(ext.getBoardData());
        boolean hasDataChange = false;
        if (body.containsKey("elements")) {
            temp.setElements(convertValue(body.get("elements"), new TypeReference<List<BoardDTO.Element>>() {}));
            hasDataChange = true;
        }
        if (body.containsKey("background")) {
            temp.setBackground((String) body.get("background"));
            hasDataChange = true;
        }

        // ============== 修复1：同步最新标题到kb_board.name ==============
        ext.setName(origin.getTitle());

        // ============== 修复2：更新扩展数据，同时同步到document.content ==============
        if (hasDataChange) {
            // 和添加逻辑完全对齐：只序列化elements+background，生成干净JSON
            String newExtJson = toJson(new BoardDTO(temp.getElements(), temp.getBackground()));
            ext.setBoardData(newExtJson);
            // 关键！回写到origin的content，主表更新时会同步到document表
            origin.setContent(newExtJson);
        }

        // 3. 更新扩展表
        kbBoardMapper.updateByDocId(ext);
    }

    private void updateMindExtension(DocumentDTO origin, Map<String, Object> body) {
        // 兼容 nodes 和 mindData 字段
        if (!body.containsKey("nodes") && !body.containsKey("mindData")) return;
        KbMind ext = kbMindMapper.selectByDocId(origin.getId());
        if (ext == null) throw new BusinessException("思维导图扩展数据不存在");
        List<MindDTO.Node> nodes = parseMindData(ext.getMindData());

        boolean hasDataChange = false;
        if (body.containsKey("nodes")) {
            nodes = convertValue(body.get("nodes"), new TypeReference<List<MindDTO.Node>>() {});
            hasDataChange = true;
        } else if (body.containsKey("mindData")) {
            nodes = convertValue(body.get("mindData"), new TypeReference<List<MindDTO.Node>>() {});
            hasDataChange = true;
        }

        // 同步修复1：更新标题到kb_mind.name
        ext.setName(origin.getTitle());
        // 同步修复2：回写最新数据到document.content
        if (hasDataChange) {
            String newExtJson = toJson(nodes);
            ext.setMindData(newExtJson);
            origin.setContent(newExtJson);
        }

        kbMindMapper.updateByDocId(ext);
    }

    private TableDTO parseTableData(String json) {
        TableDTO dto = parseJson(json, TableDTO.class);
        return dto != null ? dto : new TableDTO();
    }

    private BoardDTO parseBoardData(String json) {
        BoardDTO dto = parseJson(json, BoardDTO.class);
        return dto != null ? dto : new BoardDTO();
    }

    private List<MindDTO.Node> parseMindData(String json) {
        List<MindDTO.Node> nodes = parseJson(json, new TypeReference<List<MindDTO.Node>>() {});
        return nodes != null ? nodes : new ArrayList<>();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException("数据序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object from, TypeReference<T> typeRef) {
        return objectMapper.convertValue(from, typeRef);
    }
    /**
     * 生成唯一分享密钥（UUID 方案）
     * @return 唯一 shareKey
     */
    private String generateUniqueShareKey() {
        int maxRetries = 5; // 最大重试次数
        for (int i = 0; i < maxRetries; i++) {
            // 1. 生成 32 位无横杠的 UUID（如：a1b2c3d4e5f67890abcdef1234567890）
            String shareKey = UUID.randomUUID().toString().replace("-", "");

            // 2. 校验数据库中是否已存在该 key
            if (shareMapper.countByShareKey(shareKey) == 0) {
                return shareKey; // 不存在则直接返回
            }
            // 存在则重试（UUID 重复概率极低，重试仅为兜底）
        }
        // 3. 超过最大重试次数，抛出异常
        throw new BusinessException("生成分享链接失败，请稍后重试");
    }
}

