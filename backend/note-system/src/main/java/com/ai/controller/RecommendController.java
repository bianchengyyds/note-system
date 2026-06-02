package com.ai.controller;

import com.ai.pojo.Result;
import com.ai.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recommend")
@Slf4j
public class RecommendController {
    @Autowired
    private RecommendService recommendService;

    @GetMapping("/{type}/{limit}")
    public Result<List<?>> getRecommend(@PathVariable String type, @PathVariable Integer limit){
        log.info("获取推荐：{} {}", type, limit);
        List<?> recommendList = recommendService.getRecommend(type, limit);
        return Result.success(recommendList);
    }
}
