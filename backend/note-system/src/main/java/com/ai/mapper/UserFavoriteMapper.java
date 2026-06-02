package com.ai.mapper;

import com.ai.dto.UserFavorite;
import com.ai.dto.UserFavoriteDTO;
import com.ai.dto.UserLike;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserFavoriteMapper {
    // 判断用户是否已收藏该文档
    @Select("SELECT COUNT(*) > 0 FROM user_favorite\n" +
            "    WHERE user_id = #{currentUserId} AND doc_id = #{docId}")
    boolean existsByUserAndDoc(Long currentUserId,
                               Long docId);

    // 获取用户收藏的文档
    UserFavorite selectByUserAndTarget(Long userId, Long targetId);

    // 添加用户收藏
    void insert(UserFavorite userFavorite);

    // 删除用户收藏
    void deleteByUserAndTarget(Long userId, Long targetId);

    // 获取用户收藏的文档列表
    Page<UserFavoriteDTO> selectFavoritesByUserId(Long userId);
}
