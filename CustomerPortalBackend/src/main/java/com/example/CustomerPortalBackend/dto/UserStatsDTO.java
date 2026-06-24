package com.example.CustomerPortalBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    private long totalUsers;
    private long pendingUsers;
    private long approvedUsers;
    private long deniedUsers;
    private long blockedUsers;
}
