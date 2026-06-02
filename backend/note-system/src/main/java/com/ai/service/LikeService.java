package com.ai.service;

import com.ai.dto.TargetType;

public interface LikeService {

    void like(TargetType targetType, Long targetId);

    void unlike(TargetType targetType, Long noteId);
}
