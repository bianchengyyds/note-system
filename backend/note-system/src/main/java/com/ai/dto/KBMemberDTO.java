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
public class KBMemberDTO {
    private Long id;
    private Long kbId;
    private Long userId;
    private String name;
    private String role;
    private LocalDateTime joinedAt;
    private Integer deleted;
}