package com.ai.service;

import com.ai.dto.TargetType;

public interface FavoriteService {
    void favorite(Long noteId);

    void unFavorite(Long noteId);
}
