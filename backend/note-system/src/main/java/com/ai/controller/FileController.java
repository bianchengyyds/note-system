package com.ai.controller;

import com.ai.pojo.Result;
import com.ai.service.impl.GitHubUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private GitHubUploadService gitHubUploadService;

    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            String url = gitHubUploadService.uploadToGitHub(file);
            return Result.success(Map.of("url", url));
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}