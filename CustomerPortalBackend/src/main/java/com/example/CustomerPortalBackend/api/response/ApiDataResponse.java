package com.example.CustomerPortalBackend.api.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiDataResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;
}
