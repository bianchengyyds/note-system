package com.ai.controller;

import com.ai.annotation.OpLog;
import com.ai.dto.*;
import com.ai.exception.BusinessException;
import com.ai.pojo.PageResult;
import com.ai.pojo.Result;
import com.ai.service.DocumentService;
import com.ai.service.FavoriteService;
import com.ai.service.LikeService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/note")
@Slf4j
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FavoriteService favoriteService;


    /*
        添加文档
     */
    @PostMapping("/doc")
    @OpLog(operation = "CREATE_NOTE", targetType = "NOTE", targetId = "#result.data.id")
    public Result<DocumentDTO> addDoc(@RequestBody DocumentDTO documentDTO){
        log.info("添加文档：{}", documentDTO);
        DocumentDTO doc = documentService.addDoc(documentDTO);
        return Result.success(doc);
    }

    /*
        添加表格
     */
    @PostMapping("/table")
    @OpLog(operation = "CREATE_NOTE", targetType = "NOTE", targetId = "#result.data.id")
    public Result<TableDTO> addTable(@RequestBody TableDTO tableDTO){
        log.info("添加表格：{}", tableDTO);
        TableDTO table = documentService.addTable(tableDTO);
        return Result.success(table);
    }

    /*
        添加画板
     */
    @PostMapping("/board")
    @OpLog(operation = "CREATE_NOTE", targetType = "NOTE", targetId = "#result.data.id")
    public Result<BoardDTO> addBoard(@RequestBody BoardDTO boardDTO){
        log.info("添加画板：{}", boardDTO);
        BoardDTO board = documentService.addBoard(boardDTO);
        return Result.success(board);
    }

    /*
        添加思维导图
     */
    @PostMapping("/mind")
    @OpLog(operation = "CREATE_NOTE", targetType = "NOTE", targetId = "#result.data.id")
    public Result<MindDTO> addMind(@RequestBody MindDTO mindDTO){
        log.info("添加思维导图：{}", mindDTO);
        MindDTO mind = documentService.addMind(mindDTO);
        return Result.success(mind);
    }

    /*
        获取文档详情
     */
    @GetMapping("/{docId}")
    public Result<DocumentDTO> getDocDetailById(@PathVariable Long docId){
        log.info("获取文档详情：{}", docId);
        DocumentDTO doc = documentService.getDocDetailById(docId);
        return Result.success(doc);
    }

    /*
        修改文档
     */
    @PutMapping("/{noteId}")
    @OpLog(operation = "EDIT_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<DocumentDTO> updateNote(@PathVariable Long noteId,
                                          @RequestBody Map<String, Object> body) {
        log.info("修改文档：{}", noteId);
        DocumentDTO updated = documentService.updateNote(noteId, body);
        return Result.success(updated);
    }


    /*
        移动文档
     */
    @PutMapping("/{noteId}/move")
    @OpLog(operation = "MOVE_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<DocumentDTO> moveDoc(@PathVariable Long noteId,
                                       @RequestBody Map<String, Object> body) {
        log.info("移动文档：{}", noteId);
        // 1. 提取参数
        Long targetKbId = Long.valueOf(body.get("targetKbId").toString());
        Long targetParentDocId = body.get("targetParentDocId") != null
                ? Long.valueOf(body.get("targetParentDocId").toString())
                : null;

        // 2. 调用 Service 移动逻辑
        DocumentDTO movedDoc = documentService.moveDoc(noteId, targetKbId, targetParentDocId);

        // 3. 返回结果
        return Result.success(movedDoc);
    }

    @DeleteMapping("/{noteId}")
    @OpLog(operation = "DELETE_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<String> deleteDoc(@PathVariable Long noteId){
        log.info("删除文档：{}", noteId);
        documentService.deleteDoc(noteId);
        return Result.success();
    }

    @GetMapping("/recycle")
    public Result<PageResult<RecycleDocDTO>> getRecycleDocs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size ){
        log.info("获取回收站文档：{}", page);
        PageResult<RecycleDocDTO> pageResult = documentService.getRecycleDocs(page, size);
        return Result.success(pageResult);
    }

    @PutMapping("/{noteId}/restore")
    @OpLog(operation = "RESTORE_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<DocumentDTO> restoreDoc(@PathVariable Long noteId){
        log.info("恢复文档：{}", noteId);
        DocumentDTO doc = documentService.restoreDoc(noteId);
        return Result.success(doc);
    }

    @DeleteMapping("/{noteId}/permanent")
    @OpLog(operation = "PERMANENT_DELETE_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<String> deleteDocPermanently(@PathVariable Long noteId){
        log.info("彻底删除文档：{}", noteId);
        documentService.deleteDocPermanently(noteId);
        return Result.success();
    }

    /*
        获取笔记版本列表
     */
    @GetMapping("/{noteId}/versions")
    public Result<List<DocumentVersionDTO>> getDocVersions(@PathVariable Long noteId){
        log.info("获取文档版本：{}", noteId);
        List<DocumentVersionDTO> versions = documentService.getDocVersions(noteId);
        return Result.success(versions);
    }

    /*
        根据笔记ID和版本ID查看笔记内容
     */
    @GetMapping("/{noteId}/version/{versionId}")
    public Result<DocumentDTO> getVersionDetail(@PathVariable Long noteId, @PathVariable Long versionId) {
        log.info("获取文档版本详情：{} {}", noteId, versionId);
        DocumentDTO versionDetail = documentService.getVersionDetail(noteId, versionId);
        return Result.success(versionDetail);
    }

    /*
        回滚文档
     */
    @PostMapping("/{noteId}/rollback/{versionId}")
    @OpLog(operation = "ROLLBACK_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<DocumentDTO> rollbackDoc(@PathVariable Long noteId, @PathVariable Long versionId){
        log.info("回滚文档：{}", noteId);
        DocumentDTO doc = documentService.rollbackDoc(noteId, versionId);
        return Result.success(doc);
    }

    /*
        分享笔记
     */
    @PostMapping("/{noteId}/share")
    @OpLog(operation = "SHARE_NOTE", targetType = "NOTE", targetId = "#noteId")
    public Result<ShareDTO> shareDoc(@PathVariable Long noteId, @RequestBody ShareDTO shareDTO){
        log.info("分享笔记：{}", noteId);
        ShareDTO share = documentService.shareDoc(noteId, shareDTO);
        return Result.success(share);
    }

    /*
        导出笔记
     */
    @GetMapping("/{noteId}/export/{format}")
    public ResponseEntity<Resource> exportDoc(
            @PathVariable Long noteId,
            @PathVariable String format ){
        // 1. 校验格式是否合法
        ExportFormatEnum formatEnum = ExportFormatEnum.getByCode(format);
        if (formatEnum == null) {
            throw new BusinessException("不支持的导出格式");
        }

        // 2. 执行导出（返回文件资源）
        return documentService.exportNote(noteId, formatEnum);
    }

    /*
        点赞笔记
     */
    @PostMapping("/{noteId}/like")
    public Result<String> likeDoc(@PathVariable Long noteId){
        log.info("点赞笔记：{}", noteId);
        likeService.like(TargetType.DOC, noteId);
        return Result.success();
    }

    /*
        取消点赞笔记
     */
    @DeleteMapping("/{noteId}/like")
    public Result<String> unlikeDoc(@PathVariable Long noteId){
        log.info("取消点赞笔记：{}", noteId);
        likeService.unlike(TargetType.DOC, noteId);
        return Result.success();
    }

    /*
        收藏笔记
     */
    @PostMapping("/{noteId}/favorite")
    public Result<String> favoriteDoc(@PathVariable Long noteId){
        log.info("收藏笔记：{}", noteId);
        favoriteService.favorite(noteId);
        return Result.success();
    }

    /*
        取消收藏笔记
     */
    @DeleteMapping("/{noteId}/favorite")
    public Result<String> unFavoriteDoc(@PathVariable Long noteId){
        log.info("取消收藏笔记：{}", noteId);
        favoriteService.unFavorite(noteId);
        return Result.success();
    }

    /*
        记录笔记浏览
     */
    @PostMapping("/{noteId}/view")
    public Result<String> recordView(@PathVariable Long noteId){
        log.info("记录笔记访问：{}", noteId);
        documentService.recordView(noteId);
        return Result.success();
    }


    /*
        获取笔记评论列表
     */
    @GetMapping("/{noteId}/comments")
    public Result<List<CommentDTO>> getDocCommentsList(@PathVariable Long noteId) {
        log.info("获取笔记评论列表：noteId={}", noteId);
        List<CommentDTO> comments = documentService.getDocCommentsList(noteId);
        return Result.success(comments);
    }


}
