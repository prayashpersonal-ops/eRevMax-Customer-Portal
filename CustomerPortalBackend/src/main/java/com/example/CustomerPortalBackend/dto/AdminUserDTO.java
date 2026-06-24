package com.example.CustomerPortalBackend.dto;
import com.example.CustomerPortalBackend.enums.Provider;
import com.example.CustomerPortalBackend.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    private UUID id;
    private String name;
    private String email;
    private String companyName;
    private String address;
    private Boolean enable;
    private UserStatus status;
    private Provider provider;
    private List<AdminHotelDTO> hotels;
}
