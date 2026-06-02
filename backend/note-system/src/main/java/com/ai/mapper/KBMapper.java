package com.ai.mapper;

import com.ai.dto.DocumentDTO;
import com.ai.dto.KBDTO;
import com.ai.dto.KBMemberDTO;
import com.ai.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KBMapper {
    // 创建知识库
    Integer addKB(KBDTO kbDTO);

    // 获取知识库列表
    List<KBDTO> getKBListById(Long userId);

    // 获取知识库详情
    KBDTO getKBDetailById(Long kbId);

    List<KBMemberDTO> getMembersByKBId(Long kbId);

    List<DocumentDTO> getDocumentsByKBId(Long kbId);

    // 更新知识库
    void updateKB(KBDTO kbDTO);
    // 获取用户在知识库中的角色
    String getUserRoleInKB(@Param("kbId") Long kbId, @Param("userId") Long userId);

    // 软删除/恢复知识库
    void updateDeleted(Long kbId, Integer deleted);

    // 彻底删除知识库
    void deleteKBForce(Long kbId);

    // 获取知识库详情（包含已删除的）
    KBDTO getKBByIdIncludeDeleted(Long kbId);

    // 获取回收站中的知识库列表
    List<KBDTO> selectRecycleByUserId(Long userId);

    // 获取用户参与协作的知识库列表（不包括自己创建的）
    List<KBDTO> getCollaborationKBList(Long userId);
}