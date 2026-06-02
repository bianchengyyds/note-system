package com.ai.service.impl;

import com.ai.dto.CommentDTO;
import com.ai.exception.BusinessException;
import com.ai.mapper.CommentMapper;
import com.ai.service.CommentService;
import com.ai.service.DocumentService;
import com.ai.utils.CurrentHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private DocumentService documentService;

    @Override
    public CommentDTO add(CommentDTO commentDTO) {
        // 1. 业务校验：检查被评论的笔记是否存在
        if (documentService.getDocDetailById(commentDTO.getNoteId()) == null) {
            throw new BusinessException("被评论的笔记不存在");
        }
        // 2. 组装实体并入库
        commentDTO.setUserId(CurrentHolder.getCurrentId());
        commentDTO.setLikeCount(0);
        commentDTO.setCreatedAt(LocalDateTime.now());

        // 3. 插入评论
        commentMapper.insert(commentDTO);

        return commentDTO;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        // 1. 权限校验：只有评论作者或管理员能删除
        CommentDTO comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此评论");
        }

        // 2. 收集所有需要删除的子孙评论ID（包括自身）
        List<Long> idsToDelete = new ArrayList<>();
        collectChildIds(commentId, idsToDelete);
        idsToDelete.add(commentId);

        // 3. 批量删除
        commentMapper.deleteBatchByIds(idsToDelete);
    }

    // 递归收集子评论ID
    private void collectChildIds(Long parentId, List<Long> idList) {
        List<CommentDTO> children = commentMapper.selectByParentId(parentId);
        for (CommentDTO child : children) {
            idList.add(child.getId());
            collectChildIds(child.getId(), idList);
        }
    }

}
