package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.*;
import com.heben.clothing_s_i_server.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/today")
    public ApiResponse<TodayReport> today(@RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getTodayReport(userId));
    }

    @GetMapping("/slow-moving")
    public ApiResponse<List<SlowMovingProduct>> slowMoving(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getSlowMoving(userId));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<LowStockProduct>> lowStock(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getLowStock(userId));
    }

    @GetMapping("/top8")
    public ApiResponse<List<TopProduct>> top8(@RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getTop8(userId));
    }

    @GetMapping("/trend/7days")
    public ApiResponse<List<DailyTrend>> trend7Days(@RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getTrend7Days(userId));
    }

    @GetMapping("/peak-hours")
    public ApiResponse<List<PeakHour>> peakHours(@RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getPeakHours(userId));
    }

    @GetMapping("/customer-profile")
    public ApiResponse<List<CustomerProfile>> customerProfile(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(analyticsService.getCustomerProfile(userId));
    }
}
