package com.ai.service;

import java.util.List;

public interface RecommendService {
    List<?> getRecommend(String type, Integer limit);
}
