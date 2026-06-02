package com.ai.controller;

import com.ai.dto.SearchItemDTO;
import com.ai.dto.SearchQueryDTO;
import com.ai.pojo.PageResult;
import com.ai.pojo.Result;
import com.ai.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {
    @Autowired
    private SearchService searchService;

    /*
        全局搜索
     */
    @GetMapping // 建议路径改为此，避免与其他RESTful冲突
    public Result<PageResult<SearchItemDTO>> globalSearch(SearchQueryDTO queryDTO){
        log.info("全局搜索：{}", queryDTO);
        PageResult<SearchItemDTO> pageResult = searchService.globalSearch(queryDTO);
        return Result.success(pageResult);
    }
}
