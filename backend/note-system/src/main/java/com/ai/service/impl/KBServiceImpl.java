package com.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.ai.dto.*;
import com.ai.exception.BusinessException;
import com.ai.mapper.*;
import com.ai.pojo.PageResult;
import com.ai.service.KBService;
import com.ai.utils.CurrentHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;          // .doc 文档
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;  // .ppt 幻灯片
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KBServiceImpl implements KBService {

    @Autowired
    private KBMapper kbMapper;
    @Autowired
    private KBMemberMapper kbMemberMapper;
    @Autowired
    private UserMapper userMapper; // 用于校验用户是否存在
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private KbBoardMapper kbBoardMapper;
    @Autowired
    private KbTableMapper kbTableMapper;
    @Autowired
    private KbFileMapper kbFileMapper;
    @Autowired
    private GitHubUploadService gitHubUploadService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KbMindMapper kbMindMapper;
    @Autowired
    private EmbedMapper embedMapper;
    @Autowired
    private KBInvitationMapper kbInvitationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KBDTO addKB(KBDTO kbDTO) {
        // 1. 设置创建者ID和创建时间
        Long creatorId = CurrentHolder.getCurrentId();
        kbDTO.setCreatorId(creatorId);
        kbDTO.setCreatedAt(LocalDateTime.now());

        // 2. 先插入知识库主表（插入后 kbDTO.getId() 会自动回填）
        kbMapper.addKB(kbDTO);

        // 3. 【新增】将创建者设为 OWNER，插入成员表
        KBMemberDTO creatorMember = new KBMemberDTO();
        creatorMember.setKbId(kbDTO.getId());   // 刚生成的知识库ID
        creatorMember.setUserId(creatorId);      // 创建者ID
        creatorMember.setRole("OWNER");          // 角色：所有者
        creatorMember.setJoinedAt(LocalDateTime.now()); // 加入时间

        kbMemberMapper.insert(creatorMember);

        // 4. 返回结果
        return kbDTO;
    }

    @Override
    public PageResult<KBDTO> getKBListById(Integer page, Integer size) {
        // 获取当前登录用户 ID
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        // 设置分页参数(PageHelper)
        PageHelper.startPage(page, size);

        // 2. 执行查询
        List<KBDTO> kbList = kbMapper.getKBListById(userId);

        // 3. 强转 Page 对象获取分页信息
        Page<KBDTO> kbPage = (Page<KBDTO>) kbList;

        // 4. 封装返回（完全匹配接口文档）
        return new PageResult<>(
                kbPage.getTotal(),    // 总条数
                kbPage.getPageNum(),  // 当前页
                kbPage.getPageSize(), // 每页条数
                kbPage.getResult()
        );

    }

    @Override
    public PageResult<KBDTO> getCollaborationKBList(Integer page, Integer size) {
        // 1. 获取当前登录用户ID
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        // 2. 设置分页参数(PageHelper)
        PageHelper.startPage(page, size);

        // 3. 执行查询
        List<KBDTO> kbList = kbMapper.getCollaborationKBList(userId);

        // 4. 强转 Page 对象获取分页信息
        Page<KBDTO> kbPage = (Page<KBDTO>) kbList;

        // 5. 封装返回
        return new PageResult<>(
                kbPage.getTotal(),
                kbPage.getPageNum(),
                kbPage.getPageSize(),
                kbPage.getResult()
        );
    }

    @Override
    public KBDTO getKBDetailById(Long kbId) {
        // 1. 查询知识库基础信息
        KBDTO kbDTO = kbMapper.getKBDetailById(kbId);
        if (kbDTO == null) {
            throw new RuntimeException("知识库不存在或已删除");
        }

        // 2. 查询成员列表
        List<KBMemberDTO> members = kbMapper.getMembersByKBId(kbId);
        kbDTO.setMembers(members);

        // 权限校验核心逻辑
        checkKBPermission(kbDTO, members);


        // 3. 查询文档 + 构建树形结构
        List<DocumentDTO> docList = kbMapper.getDocumentsByKBId(kbId);
        List<DocumentDTO> docTree = buildDocTree(docList);
        kbDTO.setDocTree(docTree);

        return kbDTO;
    }

    /**
     * 知识库权限校验（私有库仅创建者/成员可访问）
     */
    private void checkKBPermission(KBDTO kbDTO, List<KBMemberDTO> members) {
        // 1. 获取【当前登录用户ID】和角色
        Long currentUserId = CurrentHolder.getCurrentId();
        String currentRole = CurrentHolder.getCurrentRole();

        // 2. 公开知识库：直接放行
        if (kbDTO.getIsPublic() == 1) {
            return;
        }

        // 3. 管理员：直接放行
        if ("ADMIN".equals(currentRole)) {
            return;
        }

        // 4. 私有知识库：校验权限
        boolean isCreator = Objects.equals(kbDTO.getCreatorId(), currentUserId);
        boolean isMember = members.stream()
                .anyMatch(member -> Objects.equals(member.getUserId(), currentUserId));

        // 既不是创建者，也不是成员 → 无权限
        if (!isCreator && !isMember) {
            throw new BusinessException(403, "无权限访问该私有知识库");
        }
    }

    /**
     * 通用文档树构建方法
     */
    private List<DocumentDTO> buildDocTree(List<DocumentDTO> docList) {
        Map<Long, DocumentDTO> docMap = new HashMap<>();
        // 初始化子节点集合
        for (DocumentDTO doc : docList) {
            doc.setChildren(new ArrayList<>());
            docMap.put(doc.getId(), doc);
        }

        List<DocumentDTO> rootList = new ArrayList<>();
        for (DocumentDTO doc : docList) {
            Long parentId = doc.getParentDocId();
            // 根节点（无父文档）
            if (parentId == null) {
                rootList.add(doc);
            } else {
                // 子节点挂载到父节点
                DocumentDTO parent = docMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(doc);
                }
            }
        }
        return rootList;
    }


    @Override
    public KBDTO updateKB(Long kbId, KBDTO kbDTO) {
        // 1. 校验知识库是否存在
        KBDTO oldKB = kbMapper.getKBDetailById(kbId);
        if (oldKB == null) {
            throw new BusinessException("知识库不存在或已删除");
        }

        // 2. 获取当前登录用户ID
        Long currentUserId = CurrentHolder.getCurrentId();

        // 3. 查询当前用户在该知识库的角色
        String userRole = kbMapper.getUserRoleInKB(kbId, currentUserId);

        // 4. 权限校验：仅 OWNER、EDITOR 拥有更新权限
        if (userRole == null) {
            throw new BusinessException("无权限：您不是该知识库的成员");
        }
        if (!"OWNER".equals(userRole) && !"EDITOR".equals(userRole)) {
            throw new BusinessException("无权限：仅创建者和编辑者可修改知识库信息");
        }

        // 5. 封装更新参数，执行更新
        kbDTO.setId(kbId);
        kbMapper.updateKB(kbDTO);

        // 6. 返回更新后的完整数据
        return kbMapper.getKBDetailById(kbId);
    }

    @Override
    public void updateDeleted(Long kbId) {
        checkKBPermission(kbId);

        kbMapper.updateDeleted(kbId, 1);

        kbMemberMapper.updateDeletedByKbId(kbId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 必须加事务
    public KBMemberDTO addKBMember(Long kbId, KBMemberDTO kbMemberDTO) {
        // Instead of directly adding the member, we now send an invitation
        // This method is kept for backward compatibility but now delegates to sendInvitation
        KBInvitationDTO invitation = sendInvitation(kbId, kbMemberDTO.getUserId(), kbMemberDTO.getRole());
        
        // Return a dummy KBMemberDTO to maintain API compatibility
        // The actual membership will be created when the invitation is accepted
        KBMemberDTO member = new KBMemberDTO();
        member.setKbId(kbId);
        member.setUserId(kbMemberDTO.getUserId());
        member.setRole(invitation.getRole());
        member.setJoinedAt(invitation.getCreatedAt()); // Use invitation creation time as placeholder
        
        return member;
    }

    @Override
    public void deleteKBMember(Long kbId, Long userId) {

        // 1. 校验知识库操作权限
        checkKBPermission(kbId);

        // 2. 查询当前成员的角色
        String role = kbMemberMapper.selectRoleByKbIdAndUserId(kbId, userId);

        // 3. 核心判断：OWNER 不能被删除
        if ("OWNER".equals(role)) {
            throw new BusinessException("所有者(OWNER)无法被移除");
        }

        // 同步软删除该知识库下所有成员
        kbMemberMapper.updateDeletedByKbIdAndUserId(kbId, userId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KBMemberDTO updateKBMemberRole(Long kbId, Long userId, KBMemberDTO kbMemberDTO) {
        // 1. 基础参数校验
        if (kbId == null || userId == null) {
            throw new RuntimeException("知识库ID/用户ID不能为空");
        }
        String role = kbMemberDTO.getRole();
        if (role == null || role.trim().isEmpty()) {
            throw new RuntimeException("角色不能为空");
        }
        // 校验角色合法性
        if (!"OWNER".equals(role) && !"EDITOR".equals(role) && !"VIEWER".equals(role)) {
            throw new RuntimeException("非法角色，仅支持 OWNER/EDITOR/VIEWER");
        }

        // 2. 获取当前登录用户ID
        Long currentUserId = CurrentHolder.getCurrentId();

        // 3. 禁止修改自己的角色
        if (currentUserId.equals(userId)) {
            throw new BusinessException("不允许修改自身角色");
        }

        // 4. 校验当前用户是否为知识库OWNER
        KBMemberDTO currentMember = kbMemberMapper.selectByKbIdAndUserId(kbId, currentUserId);
        if (currentMember == null || !"OWNER".equals(currentMember.getRole())) {
            throw new BusinessException("权限不足，仅知识库所有者可修改成员角色");
        }

        // 5. 校验待修改用户是否为知识库成员
        KBMemberDTO targetMember = kbMemberMapper.selectByKbIdAndUserId(kbId, userId);
        if (targetMember == null) {
            throw new BusinessException("该用户不是知识库成员");
        }

        // 6. 更新角色
        kbMemberMapper.updateRoleByKbIdAndUserId(kbId, userId, role);

        // 7. 封装返回结果
        targetMember.setRole(role);
        return targetMember;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreKB(Long kbId) {
        // 1. 参数校验
        if (kbId == null) {
            throw new BusinessException("知识库ID不能为空");
        }

        // 2. 获取当前登录用户ID
        Long currentUserId = CurrentHolder.getCurrentId();

        // 3. 查询知识库信息（需包含创建者ID和删除状态）
        KBDTO kb = kbMapper.getKBByIdIncludeDeleted(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        // 4. 校验知识库是否为软删除状态
        if (kb.getDeleted() == null || kb.getDeleted() != 1) {
            throw new BusinessException("知识库未删除，无需恢复");
        }

        // 5. 权限校验：仅创建者可恢复自己的知识库（对应语雀回收站仅可见自己内容）
        if (!currentUserId.equals(kb.getCreatorId())) {
            throw new BusinessException("权限不足，仅可恢复自己创建的知识库");
        }

        // 6. 恢复知识库（deleted改为0）
        kbMapper.updateDeleted(kbId, 0);

        // 7. 同步恢复该知识库下的所有成员（删除时同步软删了成员，需一并恢复）
        kbMemberMapper.updateDeletedByKbId(kbId, 0);
    }

    @Override
    public void deleteKBForce(Long kbId) {
        // 1. 基础校验：存在 + 所有者
        KBDTO oldKB = checkKBPermission(kbId);

        // 2. 核心判断：只有已软删除的数据，才能彻底删除
        if (oldKB.getDeleted() != 1) {
            throw new BusinessException("操作失败：仅可删除回收站中的知识库");
        }

        kbMemberMapper.deleteByKbIdPermanently(kbId);

        kbMapper.deleteKBForce(kbId);


    }


    /**
     * 1. 校验知识库是否存在
     * 2. 校验当前用户是否为知识库所有者
     */
    private KBDTO checkKBPermission(Long kbId) {
        // 1. 校验知识库是否存在
        // 查所有状态（包含已软删除）
        KBDTO oldKB = kbMapper.getKBByIdIncludeDeleted(kbId);
        if (oldKB == null) {
            throw new BusinessException("知识库不存在或已删除");
        }

        // 2. 获取当前登录用户ID
        Long currentUserId = CurrentHolder.getCurrentId();

        // 3. 查询当前用户在该知识库的角色
        String userRole = kbMapper.getUserRoleInKB(kbId, currentUserId);

        // 4. 权限校验：仅 OWNER 拥有删除权限
        if (!"OWNER".equals(userRole)) {
            throw new BusinessException("无权限：仅创建者可删除知识库信息");
        }
        return oldKB;
    }


    /*
        导出知识库
     */
    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public byte[] exportKB(Long kbId) {
        if (kbId == null) {
            throw new BusinessException("知识库ID不能为空");
        }

        Long currentUserId = CurrentHolder.getCurrentId();

        // 1. 权限校验：当前用户必须是该知识库成员（至少拥有 VIEWER 角色）
        KBMemberDTO member = kbMemberMapper.selectByKbIdAndUserId(kbId, currentUserId);
        if (member == null) {
            throw new BusinessException("权限不足，您不是该知识库成员");
        }

        // 2. 获取知识库基本信息（只取元数据，不需要它自带的集合字段）
        KBDTO kbDetail = kbMapper.getKBDetailById(kbId);
        if (kbDetail == null || (kbDetail.getDeleted() != null && kbDetail.getDeleted() == 1)) {
            throw new BusinessException("知识库不存在或已删除");
        }

        // 3. 独立查询成员列表
        List<KBMemberDTO> members = kbMapper.getMembersByKBId(kbId);
        if (members == null) {
            members = Collections.emptyList();
        }

        // 4. 独立查询所有笔记并构建笔记树
        List<DocumentDTO> allDocs = kbMapper.getDocumentsByKBId(kbId);
        // 确保 content 不为 null
        if (allDocs != null) {
            for (DocumentDTO doc : allDocs) {
                if (doc.getContent() == null) {
                    doc.setContent("");
                }
            }
        }

        // 5. 组装最终数据
        Map<String, Object> lakebookData = new LinkedHashMap<>();
        lakebookData.put("metadata", kbDetail);
        lakebookData.put("members", members);
        lakebookData.put("documents", allDocs);

        // 6. 序列化为 JSON 字节流
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsBytes(lakebookData);
        } catch (JsonProcessingException e) {
            throw new BusinessException("知识库导出失败：数据序列化异常");
        }
    }


    /*
        导入
     */
    @Override
    public ImportResultVO importFiles(Long kbId, MultipartFile[] files) {
        int successCount = 0;
        List<String> failList = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            try {
                // 空文件校验
                if (file.isEmpty()) {
                    failList.add(fileName + "：文件为空");
                    continue;
                }
                // 格式校验（修复无后缀bug）
                if (!fileName.contains(".")) {
                    failList.add(fileName + "：文件无后缀，不支持导入");
                    continue;
                }
                String suffix = fileName.substring(fileName.lastIndexOf("."));
                if (!FileFormatEnum.isSupported(suffix)) {
                    failList.add(fileName + "：不支持的文件格式");
                    continue;
                }

                // 核心导入逻辑（你自行扩展格式解析）
                doImportFile(kbId, file, suffix);
                successCount++;

            } catch (Exception e) {
                failList.add(fileName + "：" + e.getMessage());
            }
        }
        return new ImportResultVO(successCount, failList.size(), failList);
    }

    // ===================== 权限校验实现 =====================
    @Override
    public boolean checkEditPermission(Long kbId) {
        Long userId = CurrentHolder.getCurrentId();
        KBMemberDTO member = kbMemberMapper.selectByKbIdAndUserId(kbId, userId);
        if (member == null) {
            return false;
        }
        // 权限规则：所有者/管理员/编辑者 均有权限
        String role = member.getRole();
        return "OWNER".equals(role) || "EDITOR".equals(role);
    }

    @Override
    public PageResult<KBDTO> listRecycleKbs(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<KBDTO> kbList = kbMapper.selectRecycleByUserId(userId);
        List<KBDTO> List = kbList.stream().map(this::convertToRecycleVO).collect(Collectors.toList());
        PageInfo<KBDTO> pageInfo = new PageInfo<>(kbList);
        PageResult<KBDTO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(List);
        return result;
    }

    private KBDTO convertToRecycleVO(KBDTO kb) {
        KBDTO kbdto = new KBDTO();
        BeanUtils.copyProperties(kb, kbdto);
        return kbdto;
    }

    // ===================== 格式解析（自行扩展） =====================
    private void doImportFile(Long kbId, MultipartFile file, String suffix) throws Exception {
        // ========== 核心：使用 file 参数 获取文件基础信息 ==========
        String originalFilename = file.getOriginalFilename();
        InputStream inputStream = file.getInputStream(); // 文件流（解析必备）
        long fileSize = file.getSize();

        // ========== 核心：使用 kbId 参数 绑定到目标知识库 ==========
        // 所有格式的导入，最终都会归属到 kbId 对应的知识库

        // 根据文件格式执行对应解析逻辑
        switch (suffix.toLowerCase()) {
            // ==================== 1. 系统自有格式：无损还原 ====================
            case ".lakebook":
                // 场景：导入知识库备份文件，还原目录+文档
                // 使用：inputStream（解析文件） + kbId（归属到当前知识库）
                parseLakebook(kbId, inputStream, originalFilename);
                break;

            case ".lake":
                // 场景：单篇富文本文档
                // 使用：inputStream + kbId
                parseLake(kbId, inputStream, originalFilename);
                break;

            case ".lakeboard":
                // 看板格式
                parseLakeboard(kbId, inputStream);
                break;

            case ".laketable":
                // 表格格式
                parseLaketable(kbId, inputStream);
                break;

            // ==================== 2. 三方文档格式：解析为富文本 ====================
            case ".docx":
            case ".doc":
            case ".md":
            case ".txt":
            case ".ppt":
            case ".pptx":
                // 解析为系统富文本文档，绑定kbId
                parseDocument(kbId, inputStream, suffix, originalFilename);
                break;

            // ==================== 3. 三方表格格式：解析为在线表格 ====================
            case ".xlsx":
            case ".xls":
            case ".csv":
                parseTable(kbId, inputStream, suffix);
                break;

            // ==================== 4. 预览格式：生成预览文档 ====================
            case ".pdf":
            case ".jpg":
            case ".png":
                // 存储文件 + 生成预览，归属kbId
                savePreviewFile(kbId, file, originalFilename);
                break;

            default:
                throw new Exception("不支持的文件格式");
        }
    }

    // ==================== 1. 系统格式解析（完整实现） ====================
    /**
     * 解析 .lakebook 知识库备份文件 → 完整还原目录+文档
     */
    private void parseLakebook(Long kbId, InputStream inputStream, String fileName) {
        try {
            String backupData = IoUtil.readUtf8(inputStream);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(backupData);

            // 可选：更新知识库元数据（名称、描述等），这里暂不覆盖
            // 恢复笔记树
            JsonNode documents = root.get("documents");
            if (documents != null && documents.isArray()) {
                for (JsonNode docNode : documents) {
                    createDocumentFromJson(kbId, docNode, null);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("知识库备份文件解析失败");
        } finally {
            IoUtil.close(inputStream);
        }
    }

    /**
     * 递归创建笔记及其子节点
     */
    private void createDocumentFromJson(Long kbId, JsonNode node, Long parentDocId) {
        // 跳过根节点中的 "mind" 类型？includeMind已经包含，我们保留
        DocumentDTO doc = new DocumentDTO();
        doc.setKbId(kbId);
        doc.setTitle(node.has("title") ? node.get("title").asText("无标题") : "无标题");
        doc.setContent(node.has("content") ? node.get("content").asText("") : "");
        doc.setType(node.has("type") ? node.get("type").asText("doc") : "doc");
        doc.setParentDocId(parentDocId);
        doc.setCreatorId(CurrentHolder.getCurrentId());
        doc.setLastEditorId(CurrentHolder.getCurrentId());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        doc.setIsPublic(0); // 导入后默认私有，继承知识库权限

        documentMapper.insertKB(doc);
        Long newDocId = doc.getId();

        // 如果笔记类型是 table 或 board，需要同时创建扩展表记录（保持数据结构完整）
        if ("table".equals(doc.getType()) && doc.getContent() != null) {
            try {
                KbTable table = new KbTable();
                table.setKbId(kbId);
                table.setDocId(newDocId);
                table.setName(doc.getTitle());
                table.setTableData(doc.getContent());
                table.setCreateTime(LocalDateTime.now());
                kbTableMapper.insert(table);
            } catch (Exception e) {
                log.warn("导入表格扩展数据失败：{}", e.getMessage());
            }
        } else if ("board".equals(doc.getType()) && doc.getContent() != null) {
            try {
                KbBoard board = new KbBoard();
                board.setKbId(kbId);
                board.setDocId(newDocId);
                board.setName(doc.getTitle());
                board.setBoardData(doc.getContent());
                board.setCreateTime(LocalDateTime.now());
                kbBoardMapper.insert(board);
            } catch (Exception e) {
                log.warn("导入画板扩展数据失败：{}", e.getMessage());
            }
        }

        // 处理子节点
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                createDocumentFromJson(kbId, child, newDocId);
            }
        }
    }

    /**
     * 解析 .lake 单文档 → 无损还原富文本
     */
    private void parseLake(Long kbId, InputStream inputStream, String fileName) {
        try {
            String content = IoUtil.readUtf8(inputStream);

            DocumentDTO doc = new DocumentDTO();
            doc.setKbId(kbId);
            doc.setTitle(StrUtil.removeSuffix(fileName, ".lake"));
            doc.setContent(content);
            // ✅ 修改：.lake 文件默认视为富文本文档
            doc.setType("doc");   // 原代码：doc.setType("lake");
            doc.setParentDocId(null);
            doc.setCreatorId(CurrentHolder.getCurrentId());
            doc.setLastEditorId(CurrentHolder.getCurrentId());
            doc.setCreatedAt(LocalDateTime.now());

            documentMapper.insertKB(doc);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    /**
     * 解析 .lakeboard 看板 → 还原可编辑看板
     */
    private void parseLakeboard(Long kbId, InputStream inputStream) {
        try {
            String boardData = IoUtil.readUtf8(inputStream);

            // 1. 先创建主表记录
            DocumentDTO doc = new DocumentDTO();
            doc.setKbId(kbId);
            doc.setTitle("导入看板_" + System.currentTimeMillis());
            doc.setContent(boardData);
            // ✅ 修改：设标准看板类型
            doc.setType("board");   // 原代码缺失
            doc.setParentDocId(null);
            doc.setCreatorId(CurrentHolder.getCurrentId());
            doc.setLastEditorId(CurrentHolder.getCurrentId());
            doc.setCreatedAt(LocalDateTime.now());
            documentMapper.insertKB(doc);

            // 2. 再插入扩展表，并关联 doc_id
            KbBoard board = new KbBoard();
            board.setKbId(kbId);
            board.setDocId(doc.getId());   // 关联主表ID（确保有此字段）
            board.setName("导入看板_" + System.currentTimeMillis());
            board.setBoardData(boardData);
            board.setCreateTime(LocalDateTime.now());
            kbBoardMapper.insert(board);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    /**
     * 解析 .laketable 表格 → 还原可编辑表格
     */
    private void parseLaketable(Long kbId, InputStream inputStream) {
        try {
            String tableData = IoUtil.readUtf8(inputStream);

            // 1. 先创建主表记录
            DocumentDTO doc = new DocumentDTO();
            doc.setKbId(kbId);
            doc.setTitle("导入表格_" + System.currentTimeMillis());
            doc.setContent(tableData);
            // ✅ 修改：设标准表格类型
            doc.setType("table");   // 原代码缺失
            doc.setParentDocId(null);
            doc.setCreatorId(CurrentHolder.getCurrentId());
            doc.setLastEditorId(CurrentHolder.getCurrentId());
            doc.setCreatedAt(LocalDateTime.now());
            documentMapper.insertKB(doc);

            // 2. 再插入扩展表，关联 doc_id
            KbTable table = new KbTable();
            table.setKbId(kbId);
            table.setDocId(doc.getId());   // 确保有此字段
            table.setName("导入表格_" + System.currentTimeMillis());
            table.setTableData(tableData);
            table.setCreateTime(LocalDateTime.now());
            kbTableMapper.insert(table);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    // ==================== 2. 三方文档解析（完整实现） ====================
    /**
     * 解析 docx/md/txt/ppt → 转为富文本入库
     */
    private void parseDocument(Long kbId, InputStream inputStream, String suffix, String fileName) {
        // 1. 先插入宿主文档记录，获取 ID
        DocumentDTO doc = new DocumentDTO();
        doc.setKbId(kbId);
        doc.setTitle(StrUtil.removeSuffix(fileName, suffix));
        doc.setContent(""); 
        doc.setType("doc");
        fillRequiredFields(doc);
        documentMapper.addDoc(doc);
        Long hostDocId = doc.getId();

        String originalContent;
        // 处理 Word 文档
        if (".docx".equalsIgnoreCase(suffix)) {
            originalContent = extractTextFromDocx(kbId, hostDocId, inputStream);
        } else if (".doc".equalsIgnoreCase(suffix)) {
            originalContent = extractTextFromDoc(inputStream);
        } else if (".ppt".equalsIgnoreCase(suffix) || ".pptx".equalsIgnoreCase(suffix)) {
            originalContent = extractTextFromPpt(inputStream, suffix);
        } else {
            // .md / .txt 等纯文本
            originalContent = IoUtil.readUtf8(inputStream);
        }

        // 2. 识别并转换组件（针对 MD 或 Word 提取出的文本），同时建立关联
        String processedContent = processMarkdownTables(kbId, hostDocId, originalContent);
        processedContent = processSpecialComponents(kbId, hostDocId, processedContent);

        // 3. 更新宿主文档最终内容
        doc.setContent(processedContent);
        documentMapper.updateById(doc);
    }

    private void fillRequiredFields(DocumentDTO doc) {
        Long userId = CurrentHolder.getCurrentId();
        doc.setCreatorId(userId);
        doc.setLastEditorId(userId);
        doc.setVersion(1);
        doc.setIsPublic(0);
        doc.setViewCount(0);
        doc.setLikeCount(0);
        doc.setCommentCount(0);
        doc.setAuditStatus(1); // 直接设为审核通过
        doc.setParentDocId(null);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== 3. 表格解析（完整实现） ====================
    /**
     * 解析 excel/csv → 转为系统表格JSON入库
     */
    private void parseTable(Long kbId, InputStream inputStream, String suffix) {
        try {
            // 1. 解析表格数据为 JSON 字符串
            String tableData = parseTableToJson(inputStream, suffix);

            // 2. 先插入主表（document）记录
            DocumentDTO doc = new DocumentDTO();
            doc.setKbId(kbId);
            doc.setTitle("导入表格_" + System.currentTimeMillis());
            doc.setContent(tableData);
            doc.setType("table");
            fillRequiredFields(doc);
            documentMapper.addDoc(doc);

            // 3. 再插入扩展表
            KbTable table = new KbTable();
            table.setKbId(kbId);
            table.setDocId(doc.getId());          // 关联主表 ID
            table.setName("导入表格_" + System.currentTimeMillis());
            table.setTableData(tableData);
            table.setCreateTime(LocalDateTime.now());
            kbTableMapper.insert(table);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    // 将 Excel 转为系统表格的 JSON 格式（columns + rows）
    private String parseTableToJson(InputStream inputStream, String suffix) {
        // 1. 读取所有行数据（List<List<Object>>）
        List<List<Object>> allRows;
        if (".csv".equalsIgnoreCase(suffix)) {
            // CSV 处理：使用 Hutool 的 CsvReader
            allRows = new ArrayList<>();
            // 读取文本后按行解析（简单 csv 解析，可根据需要替换为专用库）
            String csvText = IoUtil.readUtf8(inputStream);
            String[] lines = csvText.split("\\r?\\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                List<Object> row = new ArrayList<>();
                String[] cells = line.split(",");
                for (String cell : cells) {
                    row.add(cell.trim());
                }
                allRows.add(row);
            }
        } else {
            // Excel (.xlsx / .xls) 使用 ExcelReader
            ExcelReader reader = ExcelUtil.getReader(inputStream, 0); // 默认读取第一个 sheet
//            allRows = reader.readAll(Object.class); // 每行为 List<Object>
            allRows = reader.read();

            reader.close();
        }

        if (allRows.isEmpty()) {
            return "{\"columns\":[], \"rows\":[]}";
        }

        // 2. 第一行作为列名
        List<Object> headerRow = allRows.get(0);
        List<Map<String, Object>> columns = new ArrayList<>();
        for (int i = 0; i < headerRow.size(); i++) {
            Map<String, Object> col = new HashMap<>();
            col.put("key", "col_" + i);
            col.put("title", String.valueOf(headerRow.get(i)));
            col.put("type", "text");
            columns.add(col);
        }

        // 3. 从第二行开始构造数据行
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int rowIdx = 1; rowIdx < allRows.size(); rowIdx++) {
            List<Object> rowData = allRows.get(rowIdx);
            Map<String, Object> row = new HashMap<>();
            row.put("id", "row_" + rowIdx);   // row_1 起始

            Map<String, Object> cells = new LinkedHashMap<>();
            for (int colIdx = 0; colIdx < headerRow.size(); colIdx++) {
                String key = "col_" + colIdx;
                Object val = (colIdx < rowData.size()) ? rowData.get(colIdx) : "";
                cells.put(key, val != null ? val.toString() : "");
            }
            row.put("cells", cells);
            rows.add(row);
        }

        // 4. 组装最终对象并序列化为 JSON
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);

        // 直接使用 Jackson 或 Fastjson 序列化（项目中已有 Jackson）
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new BusinessException("表格数据序列化失败");
        }
    }

    // ==================== 4. 预览文件（PDF/图片）完整实现 ====================
    /**
     * 存储PDF/图片 → 生成预览文档 + 数据库记录
     */
    private void savePreviewFile(Long kbId, MultipartFile file, String fileName) throws Exception {
        // 1. 配置存储路径（本地存储，可替换为OSS）
        String basePath = System.getProperty("user.dir") + "/upload/";
        String relativePath = kbId + "/" + System.currentTimeMillis() + "_" + fileName;
        String absolutePath = basePath + relativePath;

        // 2. 创建目录
        FileUtil.mkParentDirs(absolutePath);

        // 3. 写入文件
        file.transferTo(new File(absolutePath));

        // 4. 数据库保存记录
        KbFile kbFile = new KbFile();
        kbFile.setKbId(kbId);
        kbFile.setFileName(fileName);
        kbFile.setFilePath("/upload/" + relativePath); // 访问路径
        kbFile.setFileType(file.getContentType());
        kbFile.setFileSize(file.getSize());
        kbFile.setCreateTime(LocalDateTime.now());

        kbFileMapper.insert(kbFile);
    }

    // 提取 .docx 文本
    private String extractTextFromDocx(Long kbId, Long hostDocId, InputStream inputStream) {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            // 使用 LinkedList 按顺序存储段落元素（包括段落和表格）
            // 注意：doc.getBodyElements() 返回的 IBoyElement 需要强转判断
            List<Object> elements = new ArrayList<>();
            // XWPFDocument 的迭代器可能比较复杂，这里直接使用 getBodyElements()
            List<IBodyElement> bodyElements = doc.getBodyElements();
            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    elements.add(element);          // 段落
                } else if (element instanceof XWPFTable) {
                    elements.add(element);          // 表格
                }
            }

            StringBuilder md = new StringBuilder();
            int tableCount = 0;
            for (Object element : elements) {
                if (element instanceof XWPFParagraph) {
                    String paraMd = convertParagraphToMarkdown((XWPFParagraph) element);
                    md.append(paraMd).append("\n\n");
                } else if (element instanceof XWPFTable) {
                    // ✅ 改进：不再转为原始 Markdown，而是创建嵌套表格组件
                    String position = "word-table-" + (++tableCount);
                    Long tableId = createEmbeddedTableFromXwpf(kbId, hostDocId, (XWPFTable) element, position);
                    md.append("{{embed|table|").append(tableId).append("}}\n\n");
                }
            }
            return md.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("Word文档解析失败");
        }
    }
    private String convertParagraphToMarkdown(XWPFParagraph para) {
        // --- 处理图片（遍历所有 run） ---
        StringBuilder imageMarkdown = new StringBuilder();
        List<XWPFRun> runs = para.getRuns();
        if (runs != null) {
            for (XWPFRun run : runs) {
                // 处理嵌入式图片
                List<XWPFPicture> pictures = run.getEmbeddedPictures();
                if (pictures != null) {
                    for (XWPFPicture pic : pictures) {
                        try {
                            String imageUrl = uploadImageFromWord(pic);
                            if (imageUrl != null) {
                                imageMarkdown.append("![](").append(imageUrl).append(")\n");
                            }
                        } catch (Exception e) {
                            System.err.println("上传 Word 图片失败：" + e.getMessage());
                        }
                    }
                }
            }
        }

        // --- 标题检测（优先使用大纲级别） ---
        int outlineLevel = getOutlineLevel(para);
        if (outlineLevel > 0 && outlineLevel <= 6) {
            String prefix = "#".repeat(outlineLevel) + " ";
            return imageMarkdown.toString() + prefix + para.getText();
        }

        // 检测样式名称
        String style = para.getStyle();
        if (style != null && style.matches("Heading\\d+")) {
            int level = Integer.parseInt(style.replaceAll("[^0-9]", ""));
            level = Math.min(level, 6);
            String prefix = "#".repeat(level) + " ";
            return imageMarkdown.toString() + prefix + para.getText();
        }

        // --- 列表处理（嵌套支持） ---
        String listPrefix = getListInfo(para);
        if (listPrefix != null) {
            return imageMarkdown.toString() + listPrefix + para.getText();
        }

        // --- 普通段落（包含粗体、斜体、代码等） ---
        StringBuilder sb = new StringBuilder(imageMarkdown);
        if (runs != null) {
            for (XWPFRun run : runs) {
                String text = run.getText(0);
                if (text == null || text.isEmpty()) continue;

                boolean bold = run.isBold();
                boolean italic = run.isItalic();
                boolean isCode = run.getFontFamily() != null && run.getFontFamily().contains("Courier");

                String formatted = text;
                if (isCode) {
                    formatted = "`" + formatted + "`";
                } else {
                    if (bold && italic) {
                        formatted = "***" + formatted + "***";
                    } else if (bold) {
                        formatted = "**" + formatted + "**";
                    } else if (italic) {
                        formatted = "*" + formatted + "*";
                    }
                }
                sb.append(formatted);
            }
        }
        return sb.toString();
    }
    // 上传图片到 GitHub
    private String uploadImageFromWord(XWPFPicture pic) throws Exception {
        byte[] bytes = pic.getPictureData().getData();
        String originalName = "word_image." + pic.getPictureData().suggestFileExtension();
        
        // ✅ 修复：使用自定义的 SimpleMultipartFile 替代 MockMultipartFile
        MultipartFile file = new SimpleMultipartFile(originalName, originalName, "image/png", bytes);
        
        // 调用已有的 GitHub 上传服务
        return gitHubUploadService.uploadToGitHub(file);
    }

    // ✅ 新增：简单的 MultipartFile 实现类（用于生产环境内存文件转换）
    private static class SimpleMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public SimpleMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(File dest) {
            try {
                java.nio.file.Files.write(dest.toPath(), content);
            } catch (Exception e) {
                throw new RuntimeException("文件写入失败", e);
            }
        }
    }

    // 获取大纲级别
    private int getOutlineLevel(XWPFParagraph para) {
        CTPPr pPr = para.getCTP().getPPr();
        if (pPr != null && pPr.getOutlineLvl() != null) {
            return pPr.getOutlineLvl().getVal().intValue() + 1; // 0-based -> 1-based
        }
        return 0;
    }
    // 获取列表信息
    private String getListInfo(XWPFParagraph para) {
        BigInteger numId = para.getNumID();
        if (numId == null) return null;

        int ilvl = 0;
        try {
            ilvl = para.getNumIlvl().intValue();
        } catch (Exception ignored) {}

        String indent = "  ".repeat(ilvl);
        return indent + "- ";  // 简单处理，统一用无序列表，后续可扩展
    }
    // 表格转 Markdown
    private String convertTableToMarkdown(XWPFTable table) {
        StringBuilder md = new StringBuilder();
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return "";

        // 表头
        XWPFTableRow headerRow = rows.get(0);
        List<String> headers = new ArrayList<>();
        for (XWPFTableCell cell : headerRow.getTableCells()) {
            // 简单提取文本，忽略单元格内复杂格式
            StringBuilder cellText = new StringBuilder();
            for (XWPFParagraph p : cell.getParagraphs()) {
                cellText.append(p.getText());
            }
            headers.add(cellText.toString().trim());
        }
        md.append("| ").append(String.join(" | ", headers)).append(" |\n");
        md.append("| ").append(headers.stream().map(h -> "---").reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");

        // 数据行
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                StringBuilder cellText = new StringBuilder();
                for (XWPFParagraph p : cell.getParagraphs()) {
                    cellText.append(p.getText());
                }
                cells.add(cellText.toString().trim());
            }
            // 补齐列数
            while (cells.size() < headers.size()) {
                cells.add("");
            }
            md.append("| ").append(String.join(" | ", cells)).append(" |\n");
        }
        return md.toString();
    }

    private String extractTextFromDoc(InputStream inputStream) {
        // 先读取文件内容，判断是否为系统导出的 HTML 格式（.doc 后缀但实际是 HTML）
        byte[] bytes;
        try {
            bytes = IoUtil.readBytes(inputStream);
        } catch (Exception e) {
            throw new BusinessException("无法读取文件内容");
        }
        
        String raw = new String(bytes, StandardCharsets.UTF_8);
        if (raw.trim().startsWith("<")) {
            // HTML 格式（系统导出的 .doc 实际是 HTML），用 Jsoup 解析
            log.info("检测到 HTML 格式的 .doc 文件，使用 HTML 解析器");
            return extractTextFromHtml(raw);
        }
        
        // 真正的旧版 .doc 二进制格式
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            log.info("使用 HWPF 解析旧版 .doc 二进制文件");
            WordExtractor extractor = new WordExtractor(doc);
            return extractor.getText();
        } catch (Exception e) {
            throw new BusinessException("旧版Word文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 从 HTML 内容中提取文本及内嵌图片（data URI），
     * 保留画板/脑图等嵌套组件结构，供 processSpecialComponents 后续解析
     */
    private String extractTextFromHtml(String html) {
        Document jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().prettyPrint(false);  // 关闭格式化，避免在标签和文本间插入换行破坏正则匹配
        StringBuilder sb = new StringBuilder();

        // 遍历 body 的直接子元素
        Element body = jsoupDoc.body();
        if (body == null) {
            body = jsoupDoc;
        }

        int divCount = 0;
        for (Element child : body.children()) {
            String tag = child.tagName().toLowerCase();

            // 跳过 header/head 区域的 title
            if (tag.equals("h1") || tag.equals("h2") || tag.equals("h3") || tag.equals("h4") || tag.equals("h5") || tag.equals("h6")) {
                String prefix = "#".repeat(Integer.parseInt(tag.substring(1)));
                sb.append(prefix).append(" ").append(child.text()).append("\n\n");
            }
            // 表格
            else if (tag.equals("table")) {
                sb.append(convertHtmlTableToMarkdown(child)).append("\n\n");
            }
            // 列表
            else if (tag.equals("ul") || tag.equals("ol")) {
                sb.append(convertHtmlListToMarkdown(child)).append("\n\n");
            }
            // 代码块
            else if (tag.equals("pre")) {
                String code = child.text();
                sb.append("```\n").append(code).append("\n```\n\n");
            }
            // div（画板/脑图等嵌套组件会包含在 div 中）
            else if (tag.equals("div")) {
                // 保留原始 HTML 结构，让 processSpecialComponents 匹配
                sb.append(child.outerHtml()).append("\n\n");
                divCount++;
            }
            // 段落
            else if (tag.equals("p")) {
                String text = processHtmlTextWithImages(child);
                if (StringUtils.hasText(text)) {
                    sb.append(text).append("\n\n");
                }
            }
            // 其他元素
            else {
                String text = child.text();
                if (StringUtils.hasText(text)) {
                    sb.append(text).append("\n\n");
                }
            }
        }

        log.info("HTML 解析完成，共发现 {} 个 div 嵌套组件标签", divCount);
        return sb.toString().trim();
    }

    /**
     * 处理 HTML 元素中的文本和内嵌图片，将 img 标签保留为原始 HTML（含 data URI）
     */
    private String processHtmlTextWithImages(Element elem) {
        StringBuilder sb = new StringBuilder();
        for (org.jsoup.nodes.Node node : elem.childNodes()) {
            if (node instanceof TextNode) {
                sb.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                Element child = (Element) node;
                if ("img".equals(child.tagName().toLowerCase())) {
                    sb.append(child.outerHtml());
                } else if ("strong".equals(child.tagName().toLowerCase()) || "b".equals(child.tagName().toLowerCase())) {
                    sb.append("**").append(child.text()).append("**");
                } else if ("em".equals(child.tagName().toLowerCase()) || "i".equals(child.tagName().toLowerCase())) {
                    sb.append("*").append(child.text()).append("*");
                } else if ("code".equals(child.tagName().toLowerCase())) {
                    sb.append("`").append(child.text()).append("`");
                } else {
                    sb.append(processHtmlTextWithImages(child));
                }
            }
        }
        return sb.toString();
    }

    private String convertHtmlTableToMarkdown(Element table) {
        StringBuilder md = new StringBuilder();
        Elements rows = table.select("tr");
        if (rows.isEmpty()) return "";

        // 表头
        Elements headers = rows.first().select("th, td");
        List<String> headerCells = headers.eachText();
        md.append("| ").append(String.join(" | ", headerCells)).append(" |\n");
        md.append("| ").append(headerCells.stream().map(h -> "---").reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");

        // 数据行
        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td, th");
            List<String> cellTexts = cells.eachText();
            md.append("| ").append(String.join(" | ", cellTexts)).append(" |\n");
        }
        return md.toString();
    }

    private String convertHtmlListToMarkdown(Element list) {
        StringBuilder md = new StringBuilder();
        convertHtmlListToMarkdown(list, md, 0);
        return md.toString();
    }

    private void convertHtmlListToMarkdown(Element list, StringBuilder md, int depth) {
        boolean isOrdered = "ol".equalsIgnoreCase(list.tagName());
        int count = 1;
        for (Element li : list.children()) {
            if (!"li".equalsIgnoreCase(li.tagName())) continue;
            String indent = "  ".repeat(depth);
            if (isOrdered) {
                md.append(indent).append(count++).append(". ");
            } else {
                md.append(indent).append("- ");
            }
            // 文本内容
            md.append(li.ownText()).append("\n");
            // 嵌套子列表
            for (Element child : li.children()) {
                if ("ul".equalsIgnoreCase(child.tagName()) || "ol".equalsIgnoreCase(child.tagName())) {
                    convertHtmlListToMarkdown(child, md, depth + 1);
                }
            }
        }
    }

    // 提取 .ppt/.pptx 文本
    private String extractTextFromPpt(InputStream inputStream, String suffix) {
        if (".pptx".equalsIgnoreCase(suffix)) {
            try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
                StringBuilder sb = new StringBuilder();
                for (XSLFSlide slide : ppt.getSlides()) {
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            sb.append(((XSLFTextShape) shape).getText()).append("\n");
                        }
                    }
                }
                return sb.toString();
            } catch (Exception e) {
                throw new BusinessException("PPTX解析失败");
            }
        } else {
            // .ppt 旧格式
            try (HSLFSlideShow ppt = new HSLFSlideShow(inputStream)) {
                StringBuilder sb = new StringBuilder();
                for (HSLFSlide slide : ppt.getSlides()) {
                    for (HSLFShape shape : slide.getShapes()) {
                        if (shape instanceof HSLFTextShape) {
                            sb.append(((HSLFTextShape) shape).getText()).append("\n");
                        }
                    }
                }
                return sb.toString();
            } catch (Exception e) {
                throw new BusinessException("PPT解析失败");
            }
        }
    }

    // ==================== 5. 增强导入：表格还原逻辑 ====================

    /**
     * 从 Word 表格创建嵌套组件
     */
    private Long createEmbeddedTableFromXwpf(Long kbId, Long hostDocId, XWPFTable table, String position) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return 0L;

        // 1. 构造列
        XWPFTableRow headerRow = rows.get(0);
        List<TableDTO.Column> columns = new ArrayList<>();
        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            XWPFTableCell cell = headerRow.getTableCells().get(i);
            TableDTO.Column col = new TableDTO.Column();
            col.setKey("col_" + i);
            col.setTitle(cell.getText().trim());
            col.setType("text");
            columns.add(col);
        }

        // 2. 构造行
        List<TableDTO.Row> tableRows = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            TableDTO.Row tableRow = new TableDTO.Row();
            tableRow.setId("row_" + i);
            Map<String, Object> cells = new HashMap<>();
            for (int j = 0; j < columns.size(); j++) {
                String val = (j < row.getTableCells().size()) ? row.getTableCells().get(j).getText().trim() : "";
                cells.put(columns.get(j).getKey(), val);
            }
            tableRow.setCells(cells);
            tableRows.add(tableRow);
        }

        return saveEmbeddedTable(kbId, hostDocId, columns, tableRows, position);
    }

    /**
     * 处理 Markdown 内容中的表格，将其替换为嵌套组件占位符
     */
    private String processMarkdownTables(Long kbId, Long hostDocId, String content) {
        if (!StringUtils.hasText(content)) return content;

        // ✅ 预处理1：将 || 还原为换行符
        if (content.contains("||") && content.contains("|")) {
            content = content.replace("||", "\n");
        }
        
        // ✅ 预处理2：清理表格前后的 HTML 标签（如导出的 <p></p>），确保正则能匹配
        content = content.replaceAll("<p>\\s*</p>\\s*(\\|)", "$1");

        // 改进的 Markdown 表格匹配正则，能够更准确地识别各种表格格式
        // 匹配以 | 开头和结尾，中间有分隔符行的表格格式
        Pattern tablePattern = Pattern.compile(
            "((?:^|\\n)[ \\t]*\\|.*\\|[ \\t]*(?:\\n|$))" +           // 表头行
            "([ \\t]*\\|[ \\t]*:?[-]+:? *(?:\\|[ \\t]*:?[-]+:? *)*[ \\t]*\\|[ \\t]*(?:\\n|$))" +  // 分隔符行
            "((?:[ \\t]*\\|.*\\|[ \\t]*(?:\\n|$))*)",               // 数据行
            Pattern.MULTILINE
        );
        Matcher matcher = tablePattern.matcher(content);
        
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        int tableCount = 0;
        while (matcher.find()) {
            sb.append(content, lastEnd, matcher.start());
            String tableMd = matcher.group().trim();
            String position = "md-table-" + (++tableCount);
            Long tableId = createEmbeddedTableFromMarkdown(kbId, hostDocId, tableMd, position);
            sb.append("\n{{embed|table|").append(tableId).append("}}\n");
            lastEnd = matcher.end();
        }
        sb.append(content.substring(lastEnd));
        return sb.toString();
    }

    /**
     * 识别并转换思维导图和画板组件
     */
    private String processSpecialComponents(Long kbId, Long hostDocId, String content) {
        if (!StringUtils.hasText(content)) return content;

        // 1. 识别 HTML 格式的思维导图（带 Base64 SVG）
        Pattern htmlMindPattern = Pattern.compile(
            "<div[^>]*>\\s*<div[^>]*>\\s*思维导图:\\s*([^<]+)</div>\\s*<img src=\"data:image/svg\\+xml;base64,([^\"]+)\"[^>]*>\\s*</div>",
            Pattern.MULTILINE
        );
        Matcher htmlMindMatcher = htmlMindPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        int mindCount = 0;
        while (htmlMindMatcher.find()) {
            String title = htmlMindMatcher.group(1).trim();
            String base64Svg = htmlMindMatcher.group(2);
            log.info("发现 HTML 思维导图组件: title={}", title);
            List<MindDTO.Node> nodes = parseMindNodesFromSvg(base64Svg);
            
            String position = "mind-html-" + (++mindCount);
            Long mindId = createMindWithData(kbId, hostDocId, title, nodes, position);
            log.info("思维导图已创建: id={}, title={}", mindId, title);
            htmlMindMatcher.appendReplacement(sb, "{{embed|mind|" + mindId + "}}");
        }
        htmlMindMatcher.appendTail(sb);
        content = sb.toString();

        // 2. 识别 HTML 格式的画板（带 Base64 SVG）
        Pattern htmlBoardPattern = Pattern.compile(
            "<div[^>]*>\\s*<div[^>]*>\\s*画板:\\s*([^<]+)</div>\\s*<img src=\"data:image/svg\\+xml;base64,([^\"]+)\"[^>]*>\\s*</div>",
            Pattern.MULTILINE
        );
        Matcher htmlBoardMatcher = htmlBoardPattern.matcher(content);
        sb = new StringBuffer();
        int boardCount = 0;
        while (htmlBoardMatcher.find()) {
            String title = htmlBoardMatcher.group(1).trim();
            String base64Svg = htmlBoardMatcher.group(2);
            log.info("发现 HTML 画板组件: title={}", title);
            BoardDTO boardDTO = new BoardDTO();
            List<BoardDTO.Element> elements = parseBoardElementsFromSvg(base64Svg, boardDTO);
            
            String position = "board-html-" + (++boardCount);
            Long boardId = createBoardWithData(kbId, hostDocId, title, elements, position, boardDTO.getBackground());
            log.info("画板已创建: id={}, title={}, 元素数={}", boardId, title, elements.size());
            htmlBoardMatcher.appendReplacement(sb, "{{embed|board|" + boardId + "}}");
        }
        htmlBoardMatcher.appendTail(sb);
        content = sb.toString();

        // 3. 处理纯文本格式的思维导图 (支持 HTML 包装和纯文本格式)
        Pattern mindPattern = Pattern.compile(
            "(?:<div[^>]*>\\s*)?<div[^>]*>\\s*思维导图:\\s*([^<]+)</div>(?:\\s*<img[^>]*>)?(?:\\s*</div>)?|思维导图:\\s*([^\\n\\r!|{}<]+)", 
            Pattern.MULTILINE
        );
        Matcher mindMatcher = mindPattern.matcher(content);
        sb = new StringBuffer();
        int lastEnd = 0;
        mindCount = 0;
        while (mindMatcher.find()) {
            sb.append(content, lastEnd, mindMatcher.start());
            String title = mindMatcher.group(1) != null ? mindMatcher.group(1).trim() : mindMatcher.group(2).trim();
            
            // 尝试抓取紧随其后的 Markdown 列表数据（针对非 HTML 格式）
            List<MindDTO.Node> nodes = new ArrayList<>();
            int nextSearchStart = mindMatcher.end();
            String remaining = content.substring(nextSearchStart);
            String[] lines = remaining.split("\\r?\\n");
            int consumedLines = 0;
            if (mindMatcher.group(2) != null) { // 只有纯文本格式才抓取列表
                consumedLines = parseMindNodesFromMarkdown(lines, 0, 0, nodes);
            }

            String position = "mind-" + (++mindCount);
            Long mindId = createMindWithData(kbId, hostDocId, title, nodes, position);
            sb.append("\n{{embed|mind|").append(mindId).append("}}\n");
            
            // 计算新的偏移量
            lastEnd = nextSearchStart;
            if (consumedLines > 0) {
                // 跳过已解析的列表行
                for (int j = 0; j < consumedLines; j++) {
                    lastEnd = content.indexOf('\n', lastEnd + 1);
                }
                if (lastEnd == -1) lastEnd = content.length();
            }
        }
        sb.append(content.substring(lastEnd));
        content = sb.toString();

        // 4. 处理纯文本格式的画板 (同理支持 HTML 包装)
        Pattern boardPattern = Pattern.compile(
            "(?:<div[^>]*>\\s*)?<div[^>]*>\\s*画板:\\s*([^<]+)</div>(?:\\s*<img[^>]*>)?(?:\\s*</div>)?|画板:\\s*([^\\n\\r!|{}<]+)", 
            Pattern.MULTILINE
        );
        Matcher boardMatcher = boardPattern.matcher(content);
        sb = new StringBuffer();
        lastEnd = 0;
        boardCount = 0;
        while (boardMatcher.find()) {
            sb.append(content, lastEnd, boardMatcher.start());
            String title = boardMatcher.group(1) != null ? boardMatcher.group(1).trim() : boardMatcher.group(2).trim();
            
            String position = "board-" + (++boardCount);
            Long boardId = createEmptyBoard(kbId, hostDocId, title, position);
            sb.append("\n{{embed|board|").append(boardId).append("}}\n");
            lastEnd = boardMatcher.end();
        }
        sb.append(content.substring(lastEnd));
        
        log.info("嵌套组件处理结束: mind={}, board={}", mindCount, boardCount);
        return sb.toString();
    }

    /**
     * 从 SVG 中解析思维导图节点树
     */
    private List<MindDTO.Node> parseMindNodesFromSvg(String base64Svg) {
        try {
            String svg = new String(Base64.getDecoder().decode(base64Svg), StandardCharsets.UTF_8);
            // 匹配所有 text 标签获取节点信息 (支持浮点数坐标和 HTML 转义)
            Pattern textPattern = Pattern.compile("<text x='([\\d.]+)' y='([\\d.]+)'[^>]*>(.*?)</text>");
            Matcher matcher = textPattern.matcher(svg);
            
            class SvgNode {
                int x, y;
                String title;
                MindDTO.Node node;
            }
            
            List<SvgNode> svgNodes = new ArrayList<>();
            while (matcher.find()) {
                SvgNode sn = new SvgNode();
                sn.x = (int) Double.parseDouble(matcher.group(1));
                sn.y = (int) Double.parseDouble(matcher.group(2));
                // 还原 HTML 转义字符
                sn.title = cn.hutool.http.HtmlUtil.unescape(matcher.group(3).trim());
                sn.node = new MindDTO.Node();
                sn.node.setTitle(sn.title);
                sn.node.setChildren(new ArrayList<>());
                svgNodes.add(sn);
            }
            
            if (svgNodes.isEmpty()) return new ArrayList<>();
            
            // 按 X 坐标分组（层级），同一层级按 Y 排序
            Map<Integer, List<SvgNode>> levels = svgNodes.stream()
                .collect(Collectors.groupingBy(sn -> sn.x, TreeMap::new, Collectors.toList()));
            
            List<Integer> xCoords = new ArrayList<>(levels.keySet());
            for (Integer x : xCoords) {
                levels.get(x).sort(Comparator.comparingInt(sn -> sn.y));
            }
            
            // 建立层级关系：每一层的节点寻找上一层中 Y 距离最近的作为父节点
            for (int i = 1; i < xCoords.size(); i++) {
                List<SvgNode> currentLevel = levels.get(xCoords.get(i));
                List<SvgNode> prevLevel = levels.get(xCoords.get(i - 1));
                
                for (SvgNode current : currentLevel) {
                    SvgNode bestParent = null;
                    int minDist = Integer.MAX_VALUE;
                    for (SvgNode parent : prevLevel) {
                        int dist = Math.abs(current.y - parent.y);
                        if (dist < minDist) {
                            minDist = dist;
                            bestParent = parent;
                        }
                    }
                    if (bestParent != null) {
                        bestParent.node.getChildren().add(current.node);
                    }
                }
            }
            
            // 返回第一层（根节点层）的节点列表
            return levels.get(xCoords.get(0)).stream().map(sn -> sn.node).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("从 SVG 解析思维导图失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从 SVG 中解析画板元素列表
     */
    private List<BoardDTO.Element> parseBoardElementsFromSvg(String base64Svg, BoardDTO boardDTO) {
        List<BoardDTO.Element> elements = new ArrayList<>();
        try {
            String svg = new String(Base64.getDecoder().decode(base64Svg), StandardCharsets.UTF_8);
            
            // 0. 解析背景色
            Pattern bgPattern = Pattern.compile("<rect width='100%' height='100%' fill='([^']+)'/>");
            Matcher bgm = bgPattern.matcher(svg);
            if (bgm.find()) {
                boardDTO.setBackground(bgm.group(1));
            } else {
                boardDTO.setBackground("#ffffff");
            }
            
            // 1. 解析矩形 (并尝试解析其内部文字)
            Pattern rectPattern = Pattern.compile("<rect x='([-?\\d.]+)' y='([-?\\d.]+)' width='([-?\\d.]+)' height='([-?\\d.]+)' fill='([^']+)' stroke='([^']+)' stroke-width='([-?\\d.]+)'/>");
            Matcher rm = rectPattern.matcher(svg);
            while (rm.find()) {
                BoardDTO.Element el = new BoardDTO.Element();
                el.setType("rect");
                int rx = (int) Double.parseDouble(rm.group(1));
                int ry = (int) Double.parseDouble(rm.group(2));
                int rw = (int) Double.parseDouble(rm.group(3));
                int rh = (int) Double.parseDouble(rm.group(4));
                el.setX(rx);
                el.setY(ry);
                el.setLeft(rx);
                el.setTop(ry);
                el.setWidth(rw);
                el.setHeight(rh);
                el.setFill(rm.group(5));
                el.setStroke(rm.group(6));
                el.setStrokeWidth((int) Double.parseDouble(rm.group(7)));
                
                // 检查矩形后面是否紧跟一个居中的 text 标签 (矩形内文字)
                Pattern innerTextPattern = Pattern.compile("\\s*<text x='([-?\\d.]+)' y='([-?\\d.]+)' text-anchor='middle'[^>]*>(.*?)</text>");
                Matcher itm = innerTextPattern.matcher(svg.substring(rm.end()));
                if (itm.find() && itm.start() < 10) { // 限制距离，确保是紧跟的
                    el.setContent(cn.hutool.http.HtmlUtil.unescape(itm.group(3).trim()));
                }
                
                elements.add(el);
            }
            
            // 2. 解析圆形
            Pattern circlePattern = Pattern.compile("<circle cx='([-?\\d.]+)' cy='([-?\\d.]+)' r='([-?\\d.]+)' fill='([^']+)' stroke='([^']+)' stroke-width='([-?\\d.]+)'/>");
            Matcher cm = circlePattern.matcher(svg);
            while (cm.find()) {
                BoardDTO.Element el = new BoardDTO.Element();
                el.setType("circle");
                double cx = Double.parseDouble(cm.group(1));
                double cy = Double.parseDouble(cm.group(2));
                double r = Double.parseDouble(cm.group(3));
                int rx = (int) (cx - r);
                int ry = (int) (cy - r);
                el.setX(rx);
                el.setY(ry);
                el.setLeft(rx);
                el.setTop(ry);
                el.setWidth((int) (r * 2));
                el.setHeight((int) (r * 2));
                el.setRadius((int) r);
                el.setFill(cm.group(4));
                el.setStroke(cm.group(5));
                el.setStrokeWidth((int) Double.parseDouble(cm.group(6)));
                elements.add(el);
            }
            
            // 3. 解析线条
            Pattern linePattern = Pattern.compile("<line x1='([-?\\d.]+)' y1='([-?\\d.]+)' x2='([-?\\d.]+)' y2='([-?\\d.]+)' stroke='([^']+)' stroke-width='([-?\\d.]+)'/>");
            Matcher lm = linePattern.matcher(svg);
            while (lm.find()) {
                BoardDTO.Element el = new BoardDTO.Element();
                el.setType("line");
                int x1 = (int) Double.parseDouble(lm.group(1));
                int y1 = (int) Double.parseDouble(lm.group(2));
                int x2 = (int) Double.parseDouble(lm.group(3));
                int y2 = (int) Double.parseDouble(lm.group(4));
                el.setStartX(x1);
                el.setStartY(y1);
                el.setEndX(x2);
                el.setEndY(y2);
                el.setX(Math.min(x1, x2));
                el.setY(Math.min(y1, y2));
                el.setLeft(Math.min(x1, x2));
                el.setTop(Math.min(y1, y2));
                el.setStroke(lm.group(5));
                el.setStrokeWidth((int) Double.parseDouble(lm.group(6)));
                elements.add(el);
            }
            
            // 4. 解析独立文本 (排除掉已经作为矩形内容解析过的居中 text)
            Pattern textPattern = Pattern.compile("<text(?![^>]*text-anchor='middle')[^>]* x='([-?\\d.]+)'[^>]* y='([-?\\d.]+)'[^>]* font-size='([^']+)'[^>]* fill='([^']+)'[^>]*>(.*?)</text>");
            Matcher tm = textPattern.matcher(svg);
            while (tm.find()) {
                BoardDTO.Element el = new BoardDTO.Element();
                el.setType("i-text"); 
                int tx = (int) Double.parseDouble(tm.group(1));
                el.setX(tx);
                el.setLeft(tx);
                
                String fSizeStr = tm.group(3).replaceAll("[^0-9]", "");
                int fontSize = fSizeStr.isEmpty() ? 20 : Integer.parseInt(fSizeStr);
                
                // 还原坐标：posY = adjustedY - (int)(fontSize * 0.85)
                int adjustedY = (int) Double.parseDouble(tm.group(2));
                int ty = adjustedY - (int)(fontSize * 0.85);
                el.setY(ty);
                el.setTop(ty);
                
                el.setFontSize(fSizeStr);
                el.setFill(tm.group(4));
                String text = cn.hutool.http.HtmlUtil.unescape(tm.group(5).trim());
                el.setText(text);
                el.setContent(text);
                elements.add(el);
            }
            
            // 5. 解析路径 (画笔内容) - 从 SVG d 属性计算包围盒作为元素位置
            Pattern pathPattern = Pattern.compile("<path d='([^']+)' fill='none' stroke='([^']+)' stroke-width='([-?\\d.]+)'/>");
            Matcher pm = pathPattern.matcher(svg);
            while (pm.find()) {
                BoardDTO.Element el = new BoardDTO.Element();
                el.setType("path");
                String dAttr = pm.group(1);
                // 从 SVG 路径数据中提取包围盒原点(最小 x/y)，作为画笔元素的画布位置
                double[] bbox = computePathBoundingBox(dAttr);
                double offsetX = bbox[0];
                double offsetY = bbox[1];
                el.setX((int) offsetX);
                el.setY((int) offsetY);
                el.setLeft((int) offsetX);
                el.setTop((int) offsetY);
                // 将路径坐标减去包围盒原点，转换为相对于元素位置的坐标
                el.setContent(convertSvgPathToJson(dAttr, offsetX, offsetY));
                el.setStroke(pm.group(2));
                el.setStrokeWidth((int) Double.parseDouble(pm.group(3)));
                elements.add(el);
            }
        } catch (Exception e) {
            log.warn("从 SVG 解析画板元素失败: {}", e.getMessage());
        }
        return elements;
    }

    /**
     * 将 SVG Path D 属性字符串转换为前端 Fabric.js/Board 预期的 JSON 格式
     * 例如: "M 10 20 L 30 40" -> "[[\"M\",10,20],[\"L\",30,40]]"
     */
    private String convertSvgPathToJson(String dAttr) {
        return convertSvgPathToJson(dAttr, 0, 0);
    }

    private String convertSvgPathToJson(String dAttr, double offsetX, double offsetY) {
        if (!StringUtils.hasText(dAttr)) return "[]";

        List<List<Object>> pathData = new ArrayList<>();
        String[] parts = dAttr.split("(?=[MLQCSTAHVmlqcstahvZz])");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            List<Object> command = new ArrayList<>();
            String cmd = part.substring(0, 1);
            command.add(cmd);

            String coordsPart = part.substring(1).trim();
            if (!coordsPart.isEmpty()) {
                String[] coords = coordsPart.split("[\\s,]+");
                List<Double> nums = new ArrayList<>();
                for (String coord : coords) {
                    try {
                        nums.add(Double.parseDouble(coord));
                    } catch (NumberFormatException ignored) {}
                }
                if (Character.isUpperCase(cmd.charAt(0)) && !"Z".equalsIgnoreCase(cmd)) {
                    for (int i = 0; i + 1 < nums.size(); i += 2) {
                        command.add(nums.get(i) - offsetX);
                        command.add(nums.get(i + 1) - offsetY);
                    }
                    if (nums.size() % 2 != 0) {
                        command.add(nums.get(nums.size() - 1));
                    }
                } else {
                    for (Double n : nums) {
                        command.add(n);
                    }
                }
            }
            pathData.add(command);
        }

        try {
            return objectMapper.writeValueAsString(pathData);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 从 SVG Path D 属性字符串中计算包围盒原点(最小 x, 最小 y)
     * 用于将路径元素在导入时正确定位到画布位置
     */
    private double[] computePathBoundingBox(String dAttr) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double curX = 0, curY = 0;

        if (!StringUtils.hasText(dAttr)) return new double[]{0, 0};

        String[] parts = dAttr.split("(?=[MLQCSTAHVmlqcstahvZz])");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            char cmd = part.charAt(0);
            String coordsPart = part.substring(1).trim();
            String[] coordStrs = coordsPart.isEmpty() ? new String[0] : coordsPart.split("[\\s,]+");
            double[] nums = new double[coordStrs.length];
            for (int i = 0; i < coordStrs.length; i++) {
                try {
                    nums[i] = Double.parseDouble(coordStrs[i]);
                } catch (NumberFormatException e) {
                    nums[i] = 0;
                }
            }

            boolean isAbs = Character.isUpperCase(cmd);
            int idx = 0;

            switch (Character.toUpperCase(cmd)) {
                case 'M': // moveto
                    while (idx + 1 < nums.length) {
                        double x = isAbs ? nums[idx] : curX + nums[idx];
                        double y = isAbs ? nums[idx + 1] : curY + nums[idx + 1];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        curX = x;
                        curY = y;
                        idx += 2;
                    }
                    break;
                case 'L': // lineto
                    while (idx + 1 < nums.length) {
                        double x = isAbs ? nums[idx] : curX + nums[idx];
                        double y = isAbs ? nums[idx + 1] : curY + nums[idx + 1];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        curX = x;
                        curY = y;
                        idx += 2;
                    }
                    break;
                case 'H': // horizontal lineto
                    while (idx < nums.length) {
                        double x = isAbs ? nums[idx] : curX + nums[idx];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, curY);
                        curX = x;
                        idx++;
                    }
                    break;
                case 'V': // vertical lineto
                    while (idx < nums.length) {
                        double y = isAbs ? nums[idx] : curY + nums[idx];
                        minX = Math.min(minX, curX);
                        minY = Math.min(minY, y);
                        curY = y;
                        idx++;
                    }
                    break;
                case 'C': // cubic bezier (endpoint)
                    while (idx + 5 < nums.length) {
                        double x = isAbs ? nums[idx + 4] : curX + nums[idx + 4];
                        double y = isAbs ? nums[idx + 5] : curY + nums[idx + 5];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        curX = x;
                        curY = y;
                        idx += 6;
                    }
                    break;
                case 'S': // smooth cubic bezier (endpoint)
                case 'Q': // quadratic bezier (endpoint)
                    while (idx + 3 < nums.length) {
                        double x = isAbs ? nums[idx + 2] : curX + nums[idx + 2];
                        double y = isAbs ? nums[idx + 3] : curY + nums[idx + 3];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        curX = x;
                        curY = y;
                        idx += 4;
                    }
                    break;
                case 'T': // smooth quadratic bezier (endpoint)
                    while (idx + 1 < nums.length) {
                        double x = isAbs ? nums[idx] : curX + nums[idx];
                        double y = isAbs ? nums[idx + 1] : curY + nums[idx + 1];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        curX = x;
                        curY = y;
                        idx += 2;
                    }
                    break;
                case 'A': // arc (endpoint)
                    while (idx + 6 < nums.length) {
                        double x = isAbs ? nums[idx + 5] : curX + nums[idx + 5];
                        double y = isAbs ? nums[idx + 6] : curY + nums[idx + 6];
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        curX = x;
                        curY = y;
                        idx += 7;
                    }
                    break;
                case 'Z': // closepath (no coordinates)
                    break;
            }
        }

        if (minX == Double.MAX_VALUE) minX = 0;
        if (minY == Double.MAX_VALUE) minY = 0;
        return new double[]{minX, minY};
    }

    private Long createBoardWithData(Long kbId, Long hostDocId, String title, List<BoardDTO.Element> elements, String position, String background) {
        BoardDTO boardDTO = new BoardDTO();
        boardDTO.setElements(elements);
        boardDTO.setBackground(background != null ? background : "#ffffff");
        
        String dataJson = "{}";
        try {
            dataJson = objectMapper.writeValueAsString(boardDTO);
        } catch (Exception e) {
            log.error("画板数据序列化失败", e);
        }

        DocumentDTO doc = new DocumentDTO();
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setType("board");
        doc.setContent(dataJson);
        fillRequiredFields(doc);
        documentMapper.addDoc(doc);

        KbBoard board = new KbBoard();
        board.setDocId(doc.getId());
        board.setKbId(kbId);
        board.setBoardData(dataJson);
        board.setCreateTime(LocalDateTime.now());
        kbBoardMapper.insert(board);

        EmbedDTO embed = new EmbedDTO();
        embed.setDocId(hostDocId);
        embed.setEmbedType("board");
        embed.setNoteId(doc.getId());
        embed.setEmbedData(dataJson);
        embed.setPosition(position);
        embed.setCreatedAt(LocalDateTime.now());
        embedMapper.insert(embed);

        return doc.getId();
    }

    /**
     * 递归解析 Markdown 列表为思维导图节点
     */
    private int parseMindNodesFromMarkdown(String[] lines, int startIdx, int level, List<MindDTO.Node> parentNodes) {
        int consumed = 0;
        while (startIdx + consumed < lines.length) {
            String line = lines[startIdx + consumed];
            if (line.trim().isEmpty()) {
                consumed++;
                continue;
            }
            if (!line.trim().startsWith("- ")) {
                break;
            }

            // 计算缩进级别 (每2个空格代表一级)
            int currentIndent = 0;
            while (currentIndent < line.length() && line.charAt(currentIndent) == ' ') {
                currentIndent++;
            }
            int currentLevel = currentIndent / 2;

            if (currentLevel < level) {
                break; // 回退到上级
            }

            if (currentLevel == level) {
                MindDTO.Node node = new MindDTO.Node();
                node.setTitle(line.trim().substring(2).trim());
                node.setChildren(new ArrayList<>());
                parentNodes.add(node);
                consumed++;

                // 尝试解析子节点
                int subConsumed = parseMindNodesFromMarkdown(lines, startIdx + consumed, level + 1, node.getChildren());
                consumed += subConsumed;
            } else {
                break;
            }
        }
        return consumed;
    }

    private Long createMindWithData(Long kbId, Long hostDocId, String title, List<MindDTO.Node> nodes, String position) {
        String nodesJson = (nodes == null || nodes.isEmpty()) ? "[]" : "[]";
        if (nodes != null && !nodes.isEmpty()) {
            try {
                nodesJson = objectMapper.writeValueAsString(nodes);
            } catch (Exception e) {
                log.error("思维导图节点序列化失败", e);
            }
        }

        DocumentDTO doc = new DocumentDTO();
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setType("mind");
        doc.setContent(nodesJson);
        fillRequiredFields(doc);
        documentMapper.addDoc(doc);

        KbMind mind = new KbMind();
        mind.setDocId(doc.getId());
        mind.setKbId(kbId);
        mind.setMindData(nodesJson);
        mind.setCreatedAt(LocalDateTime.now());
        mind.setUpdatedAt(LocalDateTime.now());
        kbMindMapper.insert(mind);

        // 建立嵌套关联
        EmbedDTO embed = new EmbedDTO();
        embed.setDocId(hostDocId);
        embed.setEmbedType("mind");
        embed.setNoteId(doc.getId());
        embed.setEmbedData(nodesJson);
        embed.setPosition(position);
        embed.setCreatedAt(LocalDateTime.now());
        embedMapper.insert(embed);

        return doc.getId();
    }

    private Long createEmptyBoard(Long kbId, Long hostDocId, String title, String position) {
        DocumentDTO doc = new DocumentDTO();
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setType("board");
        doc.setContent("{}");
        fillRequiredFields(doc);
        documentMapper.addDoc(doc);

        KbBoard board = new KbBoard();
        board.setDocId(doc.getId());
        board.setKbId(kbId);
        board.setBoardData("{}");
        board.setCreateTime(LocalDateTime.now());
        kbBoardMapper.insert(board);

        // 建立嵌套关联
        EmbedDTO embed = new EmbedDTO();
        embed.setDocId(hostDocId);
        embed.setEmbedType("board");
        embed.setNoteId(doc.getId());
        embed.setEmbedData("{}");
        embed.setPosition(position);
        embed.setCreatedAt(LocalDateTime.now());
        embedMapper.insert(embed);

        return doc.getId();
    }

    /**
     * 从 Markdown 字符串创建嵌套组件
     */
    private Long createEmbeddedTableFromMarkdown(Long kbId, Long hostDocId, String tableMd, String position) {
        String[] lines = tableMd.trim().split("\\n");
        if (lines.length < 2) return 0L;

        // 1. 解析表头
        String headerLine = lines[0];
        String[] headerCells = Arrays.stream(headerLine.split("\\|"))
                .filter(s -> !s.trim().isEmpty())
                .map(String::trim)
                .toArray(String[]::new);

        List<TableDTO.Column> columns = new ArrayList<>();
        for (int i = 0; i < headerCells.length; i++) {
            TableDTO.Column col = new TableDTO.Column();
            col.setKey("col_" + i);
            col.setTitle(headerCells[i]);
            col.setType("text");
            columns.add(col);
        }

        // 2. 解析数据行 (跳过表头和分隔线)
        List<TableDTO.Row> tableRows = new ArrayList<>();
        int rowId = 1;
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            String[] cells = Arrays.stream(line.split("\\|"))
                    .filter(s -> !s.trim().isEmpty())
                    .map(String::trim)
                    .toArray(String[]::new);
            
            TableDTO.Row tableRow = new TableDTO.Row();
            tableRow.setId("row_" + (rowId++));
            Map<String, Object> cellMap = new HashMap<>();
            for (int j = 0; j < columns.size(); j++) {
                cellMap.put(columns.get(j).getKey(), (j < cells.length) ? cells[j] : "");
            }
            tableRow.setCells(cellMap);
            tableRows.add(tableRow);
        }

        return saveEmbeddedTable(kbId, hostDocId, columns, tableRows, position);
    }

    /**
     * 通用保存逻辑：插入 document, kb_table 和 document_embed
     */
    private Long saveEmbeddedTable(Long kbId, Long hostDocId, List<TableDTO.Column> columns, List<TableDTO.Row> rows, String position) {
        TableDTO tableData = new TableDTO(columns, rows);
        String json;
        try {
            json = objectMapper.writeValueAsString(tableData);
        } catch (JsonProcessingException e) {
            log.error("表格数据序列化失败", e);
            return 0L;
        }

        // 1. 插入 Document
        DocumentDTO doc = new DocumentDTO();
        doc.setKbId(kbId);
        doc.setTitle("导入的表格");
        doc.setContent(json);
        doc.setType("table");
        fillRequiredFields(doc);
        documentMapper.addDoc(doc);

        // 2. 插入 KbTable
        KbTable kbTable = new KbTable();
        kbTable.setDocId(doc.getId());
        kbTable.setKbId(kbId);
        kbTable.setName("导入的表格");
        kbTable.setTableData(json);
        kbTable.setCreateTime(LocalDateTime.now());
        kbTableMapper.insert(kbTable);

        // 3. 插入 document_embed (建立关联)
        EmbedDTO embed = new EmbedDTO();
        embed.setDocId(hostDocId);
        embed.setEmbedType("table");
        embed.setNoteId(doc.getId());
        embed.setEmbedData(json);
        embed.setPosition(position);
        embed.setCreatedAt(LocalDateTime.now());
        embedMapper.insert(embed);

        return doc.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KBInvitationDTO sendInvitation(Long kbId, Long inviteeId, String role) {
        // 1. 权限校验：仅 OWNER 可发送邀请
        Long currentUserId = CurrentHolder.getCurrentId();
        String currentUserRole = kbMapper.getUserRoleInKB(kbId, currentUserId);
        if (!"OWNER".equals(currentUserRole)) {
            throw new BusinessException("无权限：仅知识库所有者可发送邀请");
        }

        // 2. 数据合法性校验
        KBDTO kb = kbMapper.getKBDetailById(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在或已删除");
        }

        UserDTO user = userMapper.getUserById(inviteeId);
        if (user == null) {
            throw new BusinessException("邀请失败：用户不存在");
        }

        // 3. 检查用户是否已经是成员
        KBMemberDTO existingMember = kbMemberMapper.selectByKbIdAndUserIdIncludeDeleted(kbId, inviteeId);
        if (existingMember != null && (existingMember.getDeleted() == null || existingMember.getDeleted() == 0)) {
            throw new BusinessException("邀请失败：该用户已是知识库成员");
        }

        // 4. 检查是否已有待处理的邀请
        KBInvitationDTO existingInvitation = kbInvitationMapper.getPendingInvitation(kbId, inviteeId);
        if (existingInvitation != null) {
            throw new BusinessException("邀请失败：对该用户的邀请仍在等待处理中");
        }

        // 5. 创建邀请记录
        KBInvitationDTO invitation = new KBInvitationDTO();
        invitation.setKbId(kbId);
        invitation.setInviterId(currentUserId);
        invitation.setInviteeId(inviteeId);
        invitation.setRole(role);
        invitation.setStatus("PENDING");
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setUpdatedAt(LocalDateTime.now());
        // 设置过期时间为7天后
        invitation.setExpiredAt(LocalDateTime.now().plusDays(7));

        kbInvitationMapper.insert(invitation);

        return invitation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KBInvitationDTO acceptInvitation(Long invitationId) {
        // 1. 获取邀请信息
        KBInvitationDTO invitation = kbInvitationMapper.getById(invitationId);
        if (invitation == null) {
            throw new BusinessException("邀请不存在");
        }

        // 2. 检查邀请状态
        if (invitation.isAccepted()) {
            throw new BusinessException("邀请已被接受");
        }
        if (invitation.isRejected()) {
            throw new BusinessException("邀请已被拒绝");
        }
        if (invitation.isExpired()) {
            throw new BusinessException("邀请已过期");
        }

        // 3. 权限校验：只能由被邀请人接受邀请
        Long currentUserId = CurrentHolder.getCurrentId();
        if (!currentUserId.equals(invitation.getInviteeId())) {
            throw new BusinessException("权限不足：只能接受发给自己的邀请");
        }

        // 4. 检查用户是否已经是成员
        KBMemberDTO existingMember = kbMemberMapper.selectByKbIdAndUserIdIncludeDeleted(invitation.getKbId(), currentUserId);
        if (existingMember != null && (existingMember.getDeleted() == null || existingMember.getDeleted() == 0)) {
            throw new BusinessException("您已经是该知识库的成员");
        }

        // 5. 更新邀请状态为已接受
        invitation.setStatus("ACCEPTED");
        invitation.setUpdatedAt(LocalDateTime.now());
        kbInvitationMapper.update(invitation);

        // 6. 添加用户为知识库成员
        if (existingMember != null && existingMember.getDeleted() == 1) {
            // 用户之前是成员但被删除，恢复记录并更新角色
            kbMemberMapper.updateDeletedByKbIdAndUserId(invitation.getKbId(), currentUserId, 0);
            kbMemberMapper.updateRoleByKbIdAndUserId(invitation.getKbId(), currentUserId, invitation.getRole());
        } else {
            // 全新成员，插入记录
            KBMemberDTO member = new KBMemberDTO();
            member.setKbId(invitation.getKbId());
            member.setUserId(invitation.getInviteeId());
            member.setRole(invitation.getRole());
            member.setJoinedAt(LocalDateTime.now());
            member.setDeleted(0);
            kbMemberMapper.insert(member);
        }

        return invitation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KBInvitationDTO rejectInvitation(Long invitationId) {
        // 1. 获取邀请信息
        KBInvitationDTO invitation = kbInvitationMapper.getById(invitationId);
        if (invitation == null) {
            throw new BusinessException("邀请不存在");
        }

        // 2. 检查邀请状态
        if (invitation.isAccepted()) {
            throw new BusinessException("邀请已被接受，无法拒绝");
        }
        if (invitation.isRejected()) {
            throw new BusinessException("邀请已被拒绝");
        }
        if (invitation.isExpired()) {
            throw new BusinessException("邀请已过期");
        }

        // 3. 权限校验：只能由被邀请人拒绝邀请
        Long currentUserId = CurrentHolder.getCurrentId();
        if (!currentUserId.equals(invitation.getInviteeId())) {
            throw new BusinessException("权限不足：只能拒绝发给自己的邀请");
        }

        // 4. 更新邀请状态为已拒绝
        invitation.setStatus("REJECTED");
        invitation.setUpdatedAt(LocalDateTime.now());
        kbInvitationMapper.update(invitation);

        return invitation;
    }

    @Override
    public List<KBInvitationDTO> getPendingInvitations() {
        Long currentUserId = CurrentHolder.getCurrentId();
        return kbInvitationMapper.getPendingInvitationsByInviteeId(currentUserId);
    }

    @Override
    public List<KBInvitationDTO> getInvitationsByKBId(Long kbId) {
        // 权限校验: 用户必须是知识库的成员才能查看邀请列表
        Long currentUserId = CurrentHolder.getCurrentId();
        String currentUserRole = kbMapper.getUserRoleInKB(kbId, currentUserId);
        if (currentUserRole == null) {
            throw new BusinessException("权限不足：非知识库成员无法查看邀请列表");
        }

        return kbInvitationMapper.getByKbId(kbId);
    }

    @Override
    public KBInvitationDTO getInvitationById(Long invitationId) {
        KBInvitationDTO invitation = kbInvitationMapper.getById(invitationId);
        if (invitation == null) {
            throw new BusinessException("邀请不存在");
        }

        // 权限校验：只有邀请相关的用户才能查看邀请详情
        Long currentUserId = CurrentHolder.getCurrentId();
        if (!currentUserId.equals(invitation.getInviterId()) && !currentUserId.equals(invitation.getInviteeId())) {
            throw new BusinessException("权限不足：无法查看此邀请");
        }

        return invitation;
    }
}