package com.ai.mapper;

import com.ai.dto.UserLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserLikeMapper {
    // 判断用户是否已点赞
    @Select("SELECT COUNT(*) > 0 FROM user_like\n" +
            "    WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    boolean existsByUserAndTarget(@Param("userId") Long userId,
                                      @Param("targetType") String targetType,
                                      @Param("targetId") Long targetId);

    // 查询用户点赞记录
    UserLike selectByUserAndTarget(Long userId, String targetType, Long targetId);

    // 插入点赞记录
    void insert(UserLike userLike);

    // 删除点赞记录
    void deleteByUserAndTarget(Long userId, String targetType, Long targetId);
}
