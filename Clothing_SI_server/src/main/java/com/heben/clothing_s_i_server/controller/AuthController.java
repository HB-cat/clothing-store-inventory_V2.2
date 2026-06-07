package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.*;
import com.heben.clothing_s_i_server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginResponse response = authService.register(request);
            return ApiResponse.success("注册成功", response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 修改密码（Token由拦截器自动校验）
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(userId, request);
            return ApiResponse.success("密码修改成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 修改昵称/店铺名
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody UpdateProfileRequest request) {
        try {
            authService.updateProfile(userId, request.getNickname(), request.getShopName());
            return ApiResponse.success("修改成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 验证密码（智能体敏感操作安全确认）
     * POST /api/auth/verify-password
     */
    @PostMapping("/verify-password")
    public ApiResponse<Void> verifyPassword(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isBlank()) {
            return ApiResponse.badRequest("密码不能为空");
        }
        boolean ok = authService.verifyPassword(userId, password);
        if (ok) {
            return ApiResponse.success("验证成功", null);
        } else {
            return ApiResponse.badRequest("密码错误");
        }
    }
}
