package com.ai.service.impl;

import com.ai.dto.UserFavorite;
import com.ai.dto.UserLike;
import com.ai.exception.BusinessException;
import com.ai.mapper.UserFavoriteMapper;
import com.ai.service.FavoriteService;
import com.ai.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long targetId) {
        Long userId = CurrentHolder.getCurrentId();

        // 1. 防重复收藏
        UserFavorite exist = userFavoriteMapper.selectByUserAndTarget(userId, targetId);
        if (exist != null) {
            throw new BusinessException("已收藏");
        }

        // 2. 插入收藏记录
        UserFavorite userFavorite = new UserFavorite();
        userFavorite.setUserId(userId);
        userFavorite.setTargetId(targetId);
        userFavorite.setCreatedAt(LocalDateTime.now());
        userFavoriteMapper.insert(userFavorite);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unFavorite(Long targetId) {
        Long userId = CurrentHolder.getCurrentId();
        UserFavorite exist = userFavoriteMapper.selectByUserAndTarget(userId, targetId);
        if (exist == null) {
            throw new BusinessException("未收藏");
        }
        userFavoriteMapper.deleteByUserAndTarget(userId, targetId);

    }
}
