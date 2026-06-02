package com.ai.service.impl;

import com.ai.dto.DailyStatDTO;
import com.ai.mapper.DailyStatMapper;
import com.ai.mapper.UserStatisticsMapper;
import com.ai.service.DailyStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DailyStatServiceImpl implements DailyStatService {
    @Autowired
    private DailyStatMapper dailyStatMapper;

    @Override
    public DailyStatDTO refresh(LocalDate statDate) {
        DailyStatDTO statDTO = dailyStatMapper.calcDailyStat(statDate);
        dailyStatMapper.upsertDailyStat(statDTO);
        return statDTO;
    }
}
