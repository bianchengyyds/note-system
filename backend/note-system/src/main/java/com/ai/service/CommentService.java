package com.ai.service;

import com.ai.dto.CommentDTO;

public interface CommentService {
    CommentDTO add(CommentDTO commentDTO);



    void deleteComment(Long commentId, Long userId);
}
