package com.ai.controller;

import com.ai.dto.CommentDTO;
import com.ai.dto.TargetType;
import com.ai.pojo.Result;
import com.ai.service.CommentService;
import com.ai.service.LikeService;
import com.ai.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;

    /*
        添加评论
     */
    @PostMapping
    public Result<CommentDTO> add(@RequestBody CommentDTO commentDTO){
        log.info("添加评论：{}", commentDTO);
        CommentDTO data = commentService.add(commentDTO);
        return Result.success(data);
    }

    /*
        删除我的评论
     */
    @DeleteMapping("/{commentId}")
    public Result<String> deleteMyComment(@PathVariable Long commentId){
        log.info("删除我的评论：{}", commentId);
        commentService.deleteComment(commentId, CurrentHolder.getCurrentId());
        return Result.success();
    }

    /*
        点赞评论
     */
    @PostMapping("/{commentId}/like")
    public Result<String> likeComment(@PathVariable Long commentId){
        log.info("点赞评论：{}", commentId);
        likeService.like(TargetType.COMMENT, commentId);
        return Result.success();
    }

    /*
        取消点赞评论
     */
    @DeleteMapping("/{commentId}/like")
    public Result<String> unlikeComment(@PathVariable Long commentId){
        log.info("取消点赞评论：{}", commentId);
        likeService.unlike(TargetType.COMMENT, commentId);
        return Result.success();
    }


}
