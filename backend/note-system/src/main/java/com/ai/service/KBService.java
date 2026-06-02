package com.ai.service;

import com.ai.dto.ImportResultVO;
import com.ai.dto.KBDTO;
import com.ai.dto.KBMemberDTO;
import com.ai.dto.KBInvitationDTO;
import com.ai.pojo.PageResult;
import org.springframework.web.multipart.MultipartFile;

public interface KBService {
    KBDTO addKB(KBDTO kbDTO);

    PageResult<KBDTO> getKBListById(Integer page, Integer size);

    KBDTO getKBDetailById(Long kbId);

    KBDTO updateKB(Long kbId, KBDTO kbDTO);

    void updateDeleted(Long kbId);

    void deleteKBForce(Long kbId);


    KBMemberDTO addKBMember(Long kbId, KBMemberDTO kbMemberDTO);

    void deleteKBMember(Long kbId, Long userId);


    KBMemberDTO updateKBMemberRole(Long kbId, Long userId, KBMemberDTO kbMemberDTO);

    void restoreKB(Long kbId);

    byte[] exportKB(Long kbId);

    // ===================== 导入文件 =====================
    ImportResultVO importFiles(Long kbId, MultipartFile[] files);

    // ===================== 权限校验 =====================
    boolean checkEditPermission(Long kbId);

    PageResult<KBDTO> listRecycleKbs(Long userId, int page, int size);

    // 获取用户参与协作的知识库列表（不包括自己创建的）
    PageResult<KBDTO> getCollaborationKBList(Integer page, Integer size);
    
    // Invitation-related methods
    KBInvitationDTO sendInvitation(Long kbId, Long inviteeId, String role);
    KBInvitationDTO acceptInvitation(Long invitationId);
    KBInvitationDTO rejectInvitation(Long invitationId);
    java.util.List<KBInvitationDTO> getPendingInvitations();
    java.util.List<KBInvitationDTO> getInvitationsByKBId(Long kbId);
    KBInvitationDTO getInvitationById(Long invitationId);
}