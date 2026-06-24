package com.example.CustomerPortalBackend.api.response;

import com.example.CustomerPortalBackend.dto.AdminUserDTO;
import com.example.CustomerPortalBackend.dto.UserStatsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPageAbleResponse {
    private List<AdminUserDTO> content;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
    private boolean lastPage;
    private String sortBy;
    private String sortOrder;
    private UserStatsDTO stats;
}
