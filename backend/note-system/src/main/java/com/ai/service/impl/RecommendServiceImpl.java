package com.ai.service.impl;

import com.ai.dto.DocumentDTO;
import com.ai.dto.KBDTO;
import com.ai.mapper.RecommendMapper;
import com.ai.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {
    @Autowired
    private RecommendMapper recommendMapper;

    @Override
    public List<?> getRecommend(String type, Integer limit) {
        // 1. 参数校验：防止 limit 异常
        int safeLimit = validateLimit(limit);

        // 2. 根据 type 分支处理
        switch (type.toLowerCase()) { // 忽略大小写，兼容 NOTE/Kb 等写法
            case "note":
                return getHotNotes(safeLimit);
            case "kb":
                return getHotKbs(safeLimit);
            default:
                throw new IllegalArgumentException("不支持的推荐类型: " + type + "，请使用 'note' 或 'kb'");
        }
    }

    /**
     * 私有方法：获取热门笔记
     */
    private List<DocumentDTO> getHotNotes(Integer limit) {
        log.info("正在查询热门笔记，限制数量: {}", limit);
        List<DocumentDTO> notes = recommendMapper.selectHotNotes(limit);
        log.info("查询到热门笔记数量: {}", notes.size());
        return notes;
    }

    /**
     * 私有方法：获取热门知识库
     */
    private List<KBDTO> getHotKbs(Integer limit) {
        log.info("正在查询热门知识库，限制数量: {}", limit);
        List<KBDTO> kbs = recommendMapper.selectHotKbs(limit);
        log.info("查询到热门知识库数量: {}", kbs.size());
        return kbs;
    }

    /**
     * 私有方法：校验 limit 参数
     */
    private int validateLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            log.warn("limit 参数异常，使用默认值 10");
            return 10; // 默认返回 10 条
        }
        if (limit > 100) {
            log.warn("limit 参数过大 ({})，限制为最大值 100", limit);
            return 100; // 防止恶意刷数据
        }
        return limit;
    }
}
