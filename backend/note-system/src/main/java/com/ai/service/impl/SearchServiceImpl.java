package com.ai.service.impl;

import com.ai.dto.SearchItemDTO;
import com.ai.dto.SearchQueryDTO;
import com.ai.mapper.SearchMapper;
import com.ai.pojo.PageResult;
import com.ai.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private SearchMapper searchMapper;

    @Override
    public PageResult<SearchItemDTO> globalSearch(SearchQueryDTO queryDTO) {
        List<SearchItemDTO> allList = new ArrayList<>();
        String type = queryDTO.getType();
        boolean includeNotes = (type == null || type.isEmpty() || "all".equals(type)
                || "doc".equals(type) || "table".equals(type) || "board".equals(type));
        boolean includeKbs = (type == null || type.isEmpty() || "all".equals(type)
                || "KNOWLEDGE_BASE".equals(type));

        if (includeNotes) {
            // 只对 doc/table/board 单一类型传递，否则传 null 表示所有笔记
            String noteType = ("all".equals(type) || "KNOWLEDGE_BASE".equals(type)) ? null : type;
            List<SearchItemDTO> notes = searchMapper.searchDocuments(
                    queryDTO.getKeyword(), noteType,
                    queryDTO.getKbId(), queryDTO.getCreatorId(),
                    queryDTO.getStartTime(), queryDTO.getEndTime());
            allList.addAll(notes);
        }

        if (includeKbs) {
            List<SearchItemDTO> kbs = searchMapper.searchKnowledgeBases(
                    queryDTO.getKeyword(), queryDTO.getCreatorId(),
                    queryDTO.getStartTime(), queryDTO.getEndTime());
            allList.addAll(kbs);
        }

        // 按更新时间倒序排序
        allList.sort(Comparator.comparing(SearchItemDTO::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 手动分页
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 20;
        int total = allList.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<SearchItemDTO> pageList = total > 0 ? allList.subList(fromIndex, toIndex) : Collections.emptyList();

        return new PageResult<>((long) total, page, size, pageList);
    }
}