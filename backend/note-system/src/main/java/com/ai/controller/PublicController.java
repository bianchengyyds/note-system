package com.ai.controller;

import com.ai.dto.ShareAccessDTO;
import com.ai.pojo.Result;
import com.ai.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {
    @Autowired
    private DocumentService documentService;
    /*
        通过分享Key访问笔记（无需登录）
     */
    @GetMapping("/note/{shareKey}")
    public Result<ShareAccessDTO> accessNoteByShare(
            @PathVariable String shareKey,
            @RequestParam(required = false) String password) {
        ShareAccessDTO res = documentService.accessByShareKey(shareKey, password);
        return Result.success(res);
    }

}
