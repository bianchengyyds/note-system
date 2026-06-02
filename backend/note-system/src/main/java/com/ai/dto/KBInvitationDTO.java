package com.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KBInvitationDTO {
    private Long id;
    private Long kbId;
    private Long inviterId; // Who sent the invitation
    private Long inviteeId; // Who received the invitation
    private String role; // Role to assign when accepted
    private String status; // PENDING, ACCEPTED, REJECTED, EXPIRED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiredAt; // When the invitation expires
    
    // Helper methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    public boolean isAccepted() {
        return "ACCEPTED".equals(status);
    }
    
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
    
    public boolean isExpired() {
        return "EXPIRED".equals(status) || (expiredAt != null && LocalDateTime.now().isAfter(expiredAt));
    }
}