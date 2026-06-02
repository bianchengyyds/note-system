package com.ai.service;

import com.ai.dto.SearchItemDTO;
import com.ai.dto.SearchQueryDTO;
import com.ai.pojo.PageResult;

public interface SearchService {

    PageResult<SearchItemDTO> globalSearch(SearchQueryDTO queryDTO);
}
