package com.example.CustomerPortalBackend.controller;
import com.example.CustomerPortalBackend.api.request.ToggleHotelRequest;
import com.example.CustomerPortalBackend.api.response.UserPageAbleResponse;
import com.example.CustomerPortalBackend.constants.AppConstants;
import com.example.CustomerPortalBackend.dto.AdminUserDTO;
import com.example.CustomerPortalBackend.api.request.EmailRequest;
import com.example.CustomerPortalBackend.api.request.LoginRequest;
import com.example.CustomerPortalBackend.api.request.RefreshTokenRequest;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.TokenResponse;
import com.example.CustomerPortalBackend.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @PostMapping("login")
    public ResponseEntity<TokenResponse> loginAdmin(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
        return ResponseEntity.ok(adminService.logInAdmin(loginRequest,response));
    }

    @PostMapping("refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request,HttpServletResponse response){
        return ResponseEntity.ok(adminService.refreshToken(refreshTokenRequest,request, response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("seeAllUsers")
    public ResponseEntity<ApiDataResponse<UserPageAbleResponse>> seeAllUsers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USER_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder,
            @RequestParam(name = "search", defaultValue = "", required = false) String search
    ) {
        return ResponseEntity.ok(adminService.seeAllUsers(token,pageNumber,pageSize,sortBy,sortOrder,search));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("userAccessDeniedByEmail")
    public ResponseEntity<ApiResponse> userAccessDeniedByEmail(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody EmailRequest request
    ) {
        return ResponseEntity.ok(adminService.userAccessDeniedByEmail(request.email(),token));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("userAccessGrantedById")
    public ResponseEntity<ApiResponse> userAccessGrantedByEmail(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,@RequestBody EmailRequest request) {
        return ResponseEntity.ok(adminService.userAccessGrantedByEmail(request.email(),token));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){//Here can we do it logout with both refresh and access token?
        adminService.logout(request,response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("updateHotelStatus")
    public ResponseEntity<ApiResponse> updateHotelStatus(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody ToggleHotelRequest toggleHotelRequest){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(adminService.updateHotelStatus(toggleHotelRequest,token));
    }

}
