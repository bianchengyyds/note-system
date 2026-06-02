package com.ai.controller;

import com.ai.pojo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notices")
public class NoticeController {
    @GetMapping
    public Result getNotices(){
        return Result.success();
    }


}
