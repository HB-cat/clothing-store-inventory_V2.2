package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.service.BuyerFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
public class BuyerFollowController {

    private final BuyerFollowService followService;

    @PostMapping("/follow/{sellerId}")
    public ApiResponse<Void> follow(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @PathVariable Long sellerId) {
        if (role != 3) return ApiResponse.forbidden("仅买家可操作");
        try {
            followService.follow(userId, sellerId);
            return ApiResponse.success("关注成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/follow/{sellerId}")
    public ApiResponse<Void> unfollow(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @PathVariable Long sellerId) {
        if (role != 3) return ApiResponse.forbidden("仅买家可操作");
        followService.unfollow(userId, sellerId);
        return ApiResponse.success("已取消关注", null);
    }

    @GetMapping("/follows")
    public ApiResponse<List<SysUser>> getFollows(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role) {
        if (role != 3) return ApiResponse.forbidden("仅买家可访问");
        return ApiResponse.success(followService.getFollowedSellers(userId));
    }
}
