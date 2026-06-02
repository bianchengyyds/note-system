package com.ai.mapper;

import com.ai.service.DailyStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DailyStatAutoTask {

    private final DailyStatService dailyStatService;

    /**
     * 每天凌晨 1:00 自动生成前一天的全局统计数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshYesterdayStat() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            dailyStatService.refresh(yesterday);
            log.info("{} 的 daily_stat 自动刷新完成", yesterday);
        } catch (Exception e) {
            log.error("自动刷新 daily_stat 失败，日期：{}", yesterday, e);
        }
    }
}