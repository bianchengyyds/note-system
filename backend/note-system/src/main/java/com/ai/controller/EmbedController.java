package com.ai.controller;

import com.ai.dto.EmbedCreateRequest;
import com.ai.dto.EmbedCreateResponse;
import com.ai.dto.EmbedDTO;
import com.ai.pojo.Result;
import com.ai.service.EmbedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doc")
@Slf4j
public class EmbedController {
    @Autowired
    private EmbedService embedService;

    /*
        在文档中插入嵌套组件（复用同一端点，通过请求体区分）
        - 提供 noteId：直接嵌入已存在的笔记
        - 不提供 noteId，提供 newTable/newBoard/newMind：先创建组件再嵌入
     */
    @PostMapping("/{docId}/embed")
    public Result<?> insertEmbed(
            @PathVariable Long docId,
            @RequestBody EmbedCreateRequest request
    ) {
        log.info("插入嵌入关系：{}", request);
        
        // 判断是传统嵌入还是一步创建并嵌入
        if (request.getNoteId() != null) {
            // 传统嵌入：使用已存在的 noteId
            EmbedDTO embedDTO = new EmbedDTO();
            embedDTO.setNoteId(request.getNoteId());
            embedDTO.setEmbedType(request.getEmbedType());
            embedDTO.setPosition(request.getPosition());
            EmbedDTO data = embedService.insertEmbed(docId, embedDTO);
            return Result.success(data);
        } else {
            // 一步创建并嵌入
            EmbedCreateResponse data = embedService.createAndEmbed(docId, request);
            return Result.success(data);
        }
    }

    /*
        删除嵌入关系
     */
    @DeleteMapping("/{docId}/embed/{embedId}")
    public Result<String> deleteEmbed(@PathVariable Long docId, @PathVariable Long embedId){
        log.info("删除嵌入关系：{}", embedId);
        embedService.deleteEmbed(docId, embedId);
        return Result.success();
    }

    /*
        获取嵌入关系（增强版）
        - includeData: 是否返回完整内容数据
     */
    @GetMapping("/{docId}/embeds")
    public Result<List<EmbedDTO>> getEmbeds(
            @PathVariable Long docId,
            @RequestParam(defaultValue = "false") boolean includeData){
        log.info("获取嵌入关系列表：{}, includeData={}", docId, includeData);
        List<EmbedDTO> dataList = embedService.getEmbeds(docId, includeData);
        return Result.success(dataList);
    }
}