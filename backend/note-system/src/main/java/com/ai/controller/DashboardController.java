package com.ai.controller;

import com.ai.dto.DashboardDTO;
import com.ai.pojo.Result;
import com.ai.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public Result<DashboardDTO> getDashboardInfo(){
        log.info("获取首页信息");
        DashboardDTO dashboardDTO = dashboardService.getDashboardInfo();
        return Result.success(dashboardDTO);
    }


}
