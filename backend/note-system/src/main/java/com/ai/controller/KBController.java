package com.ai.controller;

import com.ai.annotation.OpLog;
import com.ai.dto.ImportResultVO;
import com.ai.dto.KBDTO;
import com.ai.dto.KBMemberDTO;
import com.ai.dto.KBInvitationDTO;
import com.ai.mapper.KBMapper;
import com.ai.pojo.PageResult;
import com.ai.pojo.Result;
import com.ai.service.KBService;
import com.ai.utils.CurrentHolder;
import com.aliyun.core.annotation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/kb")
public class KBController {

    @Autowired
    private KBService kbService;

    /**
     * 创建知识库
     */
    @PostMapping
    @OpLog(operation = "CREATE_KB", targetType = "KB", targetId = "#result.data.id")
    public Result<KBDTO> addKB(@RequestBody KBDTO kbDTO){
        log.info("创建知识库: {}", kbDTO);

        // 调用Service层，返回包含自动生成字段（id、createdAt）的完整对象
        KBDTO savedKb = kbService.addKB(kbDTO);

        // 返回带数据的响应
        return Result.success(savedKb);
    }

    /*
        获取我的知识库列表
     */
    @GetMapping("/my")
    public Result<PageResult<KBDTO>> getMyKBList(
            // 前端不传则默认第1页
            @RequestParam(defaultValue = "1") Integer page,
            // 前端不传则默认20条/页
            @RequestParam(defaultValue = "20") Integer size
    ){
        log.info("获取我的知识库列表");
        PageResult<KBDTO> pageResult = kbService.getKBListById(page, size);
        return Result.success(pageResult);
    }

    /*
        获取协作知识库列表（不包括自己创建的）
     */
    @GetMapping("/collaborations")
    public Result<PageResult<KBDTO>> getCollaborationKBList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ){
        log.info("获取协作知识库列表");
        PageResult<KBDTO> pageResult = kbService.getCollaborationKBList(page, size);
        return Result.success(pageResult);
    }

    /*
        获取知识库详情（含文档树）
     */
    @GetMapping("/{kbId}")
    public Result<KBDTO> getKBDetail(@PathVariable Long kbId){
        KBDTO kbDetail = kbService.getKBDetailById(kbId);
        return Result.success(kbDetail);
    }

    /*
        更新知识库
     */
    @PutMapping("/{kbId}")
    @OpLog(operation = "EDIT_KB", targetType = "KB", targetId = "#kbId")
    public Result<KBDTO> updateKB(@PathVariable Long kbId, @RequestBody KBDTO kbDTO){
        log.info("更新知识库: {}", kbDTO);
        KBDTO kb = kbService.updateKB(kbId, kbDTO);
        return Result.success(kb);
    }

    /*
        软删除知识库
     */
    @DeleteMapping("/{kbId}")
    @OpLog(operation = "DELETE_KB", targetType = "KB", targetId = "#kbId")
    public Result<String> updateDeleted(@PathVariable Long kbId){
        log.info("软删除知识库: {}", kbId);
        kbService.updateDeleted(kbId);
        return Result.success();
    }

    /*
        添加知识库成员
     */
    @PostMapping("/{kbId}/members")
    @OpLog(operation = "ADD_KB_MEMBER", targetType = "KB", targetId = "#kbId")
    public Result<KBMemberDTO> addKBMember(@PathVariable Long kbId, @RequestBody KBMemberDTO kbMemberDTO){
        log.info("添加知识库成员: {}", kbId);
        KBMemberDTO kbMember = kbService.addKBMember(kbId, kbMemberDTO);
        return Result.success(kbMember);
    }

    /*
        删除知识库成员
     */
    @DeleteMapping("/{kbId}/members/{userId}")
    @OpLog(operation = "REMOVE_KB_MEMBER", targetType = "KB", targetId = "#kbId")
    public Result<String> deleteKBMember(@PathVariable Long kbId, @PathVariable Long userId){
        log.info("删除知识库成员: {}", kbId);
        kbService.deleteKBMember(kbId, userId);
        return Result.success();
    }

    /*
        更新知识库成员角色
     */
    @PutMapping("/{kbId}/members/{userId}")
    @OpLog(operation = "UPDATE_KB_MEMBER_ROLE", targetType = "KB", targetId = "#kbId")
    public Result<KBMemberDTO> updateKBMemberRole(@PathVariable Long kbId, @PathVariable Long userId, @RequestBody KBMemberDTO kbMemberDTO){
        log.info("更新知识库成员角色");
        KBMemberDTO kbMember = kbService.updateKBMemberRole(kbId, userId, kbMemberDTO);
        return Result.success(kbMember);
    }


    /*
        彻底删除知识库
     */
    @DeleteMapping("/{kbId}/permanent")
    @OpLog(operation = "PERMANENT_DELETE_KB", targetType = "KB", targetId = "#kbId")
    public Result<String> deleteKBForce(@PathVariable Long kbId){
        log.info("彻底删除知识库: {}", kbId);
        kbService.deleteKBForce(kbId);
        return Result.success();
    }


    /*
        恢复知识库
     */
    @PutMapping("/{kbId}/restore")
    @OpLog(operation = "RESTORE_KB", targetType = "KB", targetId = "#kbId")
    public Result<String> restoreKB(@PathVariable Long kbId){
        log.info("恢复知识库: {}", kbId);
        kbService.restoreKB(kbId);
        return Result.success();
    }


    /**
     * 知识库全量导出
     * 成功：返回 .lakebook 文件流
     * 失败：返回统一 JSON 格式 {code: xxx, msg: xxx, data: null}
     */
    @GetMapping("/{kbId}/export")
    @OpLog(operation = "EXPORT_KB", targetType = "KB", targetId = "#kbId")
    public ResponseEntity<?> exportKB(@PathVariable Long kbId) {
        try {
            // 1. 调用 Service 层获取导出数据
            byte[] lakebookData = kbService.exportKB(kbId);

            // 2. 构建文件下载响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            // 3. 设置下载文件名（防止中文乱码）
            String filename = "知识库全量导出_" + System.currentTimeMillis() + ".lakebook";
            filename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(lakebookData.length);

            // 4. 返回文件流（成功时无 JSON 响应体）
            return new ResponseEntity<>(lakebookData, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            // 5. 异常时返回统一 JSON 格式
            String message = e.getMessage();
            if (message != null && (message.contains("权限不足") || message.contains("无该知识库查看权限"))) {
                // 权限不足返回 403 状态码 + 统一 JSON
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Result.error(message));
            }
            // 其他业务异常返回 500 状态码 + 统一 JSON
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(message));
        } catch (UnsupportedEncodingException e) {
            // 文件名编码异常
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("文件名编码失败"));
        }
    }


    /*
        导入文档(多种类型)到知识库
     */
    @PostMapping("/{kbId}/import")
    @OpLog(operation = "IMPORT_KB", targetType = "KB", targetId = "#kbId")
    public Result<ImportResultVO> importKbFile(
            @PathVariable Long kbId,
            @RequestParam("files") MultipartFile[] files
    ) {
        // 入参校验
        if (files == null || files.length == 0) {
            return Result.error("请上传文件");
        }
        // 权限校验
        if (!kbService.checkEditPermission(kbId)) {
            return Result.error("无权限操作");
        }
        // 执行导入
        ImportResultVO result = kbService.importFiles(kbId, files);
        return Result.success(result);
    }

    /*
        获取回收站中的知识库列表
     */
    @GetMapping("/recycle")
    public Result<PageResult<KBDTO>> getRecycleKbs(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size) {
        Long userId = CurrentHolder.getCurrentId(); // 你的 token 解析工具类
        PageResult<KBDTO> pageResult = kbService.listRecycleKbs(userId, page, size);
        return Result.success(pageResult);
    }

    /*
        发送知识库邀请
     */
    @PostMapping("/{kbId}/invitations")
    @OpLog(operation = "SEND_INVITATION", targetType = "KB", targetId = "#kbId")
    public Result<KBInvitationDTO> sendInvitation(@PathVariable Long kbId, @RequestBody Map<String, Object> request){
        log.info("发送知识库邀请: {}", kbId);
        Long inviteeId = Long.valueOf(request.get("userId").toString());
        String role = request.get("role").toString();
        KBInvitationDTO invitation = kbService.sendInvitation(kbId, inviteeId, role);
        return Result.success(invitation);
    }

    /*
        接受知识库邀请
     */
    @PutMapping("/invitations/{invitationId}/accept")
    public Result<KBInvitationDTO> acceptInvitation(@PathVariable Long invitationId){
        log.info("接受知识库邀请: {}", invitationId);
        KBInvitationDTO invitation = kbService.acceptInvitation(invitationId);
        return Result.success(invitation);
    }

    /*
        拒绝知识库邀请
     */
    @PutMapping("/invitations/{invitationId}/reject")
    public Result<KBInvitationDTO> rejectInvitation(@PathVariable Long invitationId){
        log.info("拒绝知识库邀请: {}", invitationId);
        KBInvitationDTO invitation = kbService.rejectInvitation(invitationId);
        return Result.success(invitation);
    }

    /*
        获取用户待处理的邀请列表
     */
    @GetMapping("/invitations/pending")
    public Result<List<KBInvitationDTO>> getPendingInvitations(){
        log.info("获取用户待处理的邀请列表");
        List<KBInvitationDTO> invitations = kbService.getPendingInvitations();
        return Result.success(invitations);
    }

    /*
        获取知识库的邀请列表
     */
    @GetMapping("/{kbId}/invitations")
    public Result<List<KBInvitationDTO>> getInvitationsByKBId(@PathVariable Long kbId){
        log.info("获取知识库的邀请列表: {}", kbId);
        List<KBInvitationDTO> invitations = kbService.getInvitationsByKBId(kbId);
        return Result.success(invitations);
    }

}