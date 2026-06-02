package com.ai.mapper;

import com.ai.dto.DailyStatDTO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface DailyStatMapper {

    // 刷新指定日期的每日统计
    DailyStatDTO calcDailyStat(LocalDate statDate);

    // 插入或更新每日统计
    void upsertDailyStat(DailyStatDTO statDTO);
}
