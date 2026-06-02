package com.ai.mapper;

import com.ai.dto.KBMemberDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KBMemberMapper {

    // 根据知识库ID和用户ID查询成员（用于重复校验）
    KBMemberDTO selectByKbIdAndUserId(@Param("kbId") Long kbId, @Param("userId") Long userId);


    KBMemberDTO selectByKbIdAndUserIdIncludeDeleted(@Param("kbId") Long kbId, @Param("userId") Long userId);

    // 插入
    void insert(KBMemberDTO kbMemberDTO);

    // 根据知识库ID软删除成员
    void updateDeletedByKbIdAndUserId(@Param("kbId") Long kbId, @Param("userId") Long userId, @Param("deleted") Integer deleted);

    // 查询成员角色
    String selectRoleByKbIdAndUserId(
            @Param("kbId") Long kbId,
            @Param("userId") Long userId
    );

    // 根据知识库ID软删除所有成员
    void updateDeletedByKbId(
            @Param("kbId") Long kbId,
            @Param("deleted") Integer deleted
    );

    // 新增：根据知识库ID 物理删除所有成员
    void deleteByKbIdPermanently(@Param("kbId") Long kbId);


    // 更新成员角色
    void updateRoleByKbIdAndUserId(@Param("kbId") Long kbId, @Param("userId") Long userId, @Param("role") String role);

    // KbMemberMapper.java
    // 根据kbId查询所有成员（用于导出成员列表）
    List<KBMemberDTO> selectListByKbId(@Param("kbId") Long kbId);


}
