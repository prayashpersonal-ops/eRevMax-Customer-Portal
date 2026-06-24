package com.example.CustomerPortalBackend.api.request;

import com.example.CustomerPortalBackend.dto.HotelsDTO;

import java.util.List;

public record AssignHotelsToUser(
        String email,
        List<HotelsDTO> hotels
) {
}
