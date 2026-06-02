package com.ai.mapper;

import com.ai.dto.CommentDTO;
import com.ai.dto.ReplyDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {
    // 插入评论
    void insert(CommentDTO commentDTO);

    // 评论点赞数加1
    void incrementLikeCount(Long targetId);

    // 评论点赞数减1
    void decrementLikeCount(Long targetId);

    // 根据ID查询评论
    CommentDTO selectById(Long commentId);



    List<CommentDTO> selectAllCommentsByNoteId(Long noteId);

    // 根据ID删除评论
    // 根据父ID查询子评论（只需 id）
    @Select("SELECT id FROM comment WHERE parent_comment_id = #{parentId}")
    List<CommentDTO> selectByParentId(@Param("parentId") Long parentId);

    // 批量删除
    @Delete("<script>DELETE FROM comment WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteBatchByIds(@Param("ids") List<Long> ids);
}
