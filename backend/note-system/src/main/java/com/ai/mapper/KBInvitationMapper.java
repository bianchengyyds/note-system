package com.ai.mapper;

import com.ai.dto.KBInvitationDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KBInvitationMapper {

    // Insert a new invitation
    void insert(KBInvitationDTO invitation);

    // Update an existing invitation
    void update(KBInvitationDTO invitation);

    // Get invitation by ID
    KBInvitationDTO getById(@Param("id") Long id);

    // Get pending invitations for a specific user
    List<KBInvitationDTO> getPendingInvitationsByInviteeId(@Param("inviteeId") Long inviteeId);

    // Get invitations by knowledge base ID
    List<KBInvitationDTO> getByKbId(@Param("kbId") Long kbId);

    // Get invitation by knowledge base ID and invitee ID
    KBInvitationDTO getByKbIdAndInviteeId(@Param("kbId") Long kbId, @Param("inviteeId") Long inviteeId);

    // Check if there is a pending invitation for a user to join a knowledge base
    KBInvitationDTO getPendingInvitation(@Param("kbId") Long kbId, @Param("inviteeId") Long inviteeId);
}