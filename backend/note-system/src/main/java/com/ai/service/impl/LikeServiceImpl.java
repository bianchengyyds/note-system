package com.ai.service.impl;

import com.ai.dto.TargetType;
import com.ai.dto.UserLike;
import com.ai.exception.BusinessException;
import com.ai.mapper.CommentMapper;
import com.ai.mapper.DocumentMapper;
import com.ai.mapper.UserLikeMapper;
import com.ai.service.LikeService;
import com.ai.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LikeServiceImpl implements LikeService {
    @Autowired
    private UserLikeMapper userLikeMapper;
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private CommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(TargetType targetType, Long targetId) {
        Long userId = CurrentHolder.getCurrentId();
        String typeStr = targetType.name();

        // 1. 防重复点赞
        UserLike exist = userLikeMapper.selectByUserAndTarget(userId, typeStr, targetId);
        if (exist != null) {
            throw new BusinessException("已点赞");
        }

        // 2. 插入点赞记录
        UserLike userLike = new UserLike();
        userLike.setUserId(userId);
        userLike.setTargetType(typeStr);
        userLike.setTargetId(targetId);
        userLike.setCreatedAt(LocalDateTime.now());
        userLikeMapper.insert(userLike);

        // 3. 根据类型更新点赞数
        if (targetType == TargetType.DOC) {
            // DOC：更新主表 document（覆盖文档/表格/看板/思维导图）
            documentMapper.incrementLikeCount(targetId);
        } else if (targetType == TargetType.COMMENT) {
            // COMMENT：更新评论表
            commentMapper.incrementLikeCount(targetId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(TargetType targetType, Long targetId) {
        Long userId = CurrentHolder.getCurrentId();
        String type = targetType.name();

        // 1. 查询是否已点赞
        UserLike exist = userLikeMapper.selectByUserAndTarget(userId, type, targetId);
        if (exist == null) {
            throw new BusinessException("未点赞，无法取消");
        }

        // 2. 删除点赞记录
        userLikeMapper.deleteByUserAndTarget(userId, type, targetId);

        // 3. 更新对应表的点赞数 -1
        if (targetType == TargetType.DOC) {
            documentMapper.decrementLikeCount(targetId);
        } else if (targetType == TargetType.COMMENT) {
            commentMapper.decrementLikeCount(targetId);
        }
    }
}
