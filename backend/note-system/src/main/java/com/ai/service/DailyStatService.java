package com.ai.service;

import com.ai.dto.DailyStatDTO;

import java.time.LocalDate;

public interface DailyStatService {
    // 刷新指定日期的每日统计
    DailyStatDTO refresh(LocalDate statDate);
}
