package com.ai.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class GitHubUploadService {

    @Value("${github.token}")
    private String token;

    @Value("${github.owner}")
    private String owner;

    @Value("${github.repo}")
    private String repo;

    @Value("${github.branch}")
    private String branch;

    @Value("${github.upload-path}")
    private String uploadPath;

    @Value("${github.access-url-prefix}")
    private String accessUrlPrefix;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadToGitHub(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(bytes);

        String originalName = file.getOriginalFilename();
        String suffix = originalName != null ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String newName = UUID.randomUUID().toString() + suffix;

        String path = uploadPath + newName;
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

        Map<String, String> body = new HashMap<>();
        body.put("message", "Upload image: " + newName);
        body.put("content", base64Content);
        body.put("branch", branch);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.trim());   // ← 改进
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Map.class);

        if (response.getBody() != null && response.getBody().get("content") != null) {
            Map<String, Object> content = (Map<String, Object>) response.getBody().get("content");
            String downloadUrl = (String) content.get("download_url");
            if (downloadUrl != null) return downloadUrl;
        }
        throw new RuntimeException("GitHub 响应异常");
    }
}