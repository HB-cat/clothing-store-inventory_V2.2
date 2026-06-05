package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.Appointment;
import com.heben.clothing_s_i_server.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ApiResponse<Appointment> create(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @RequestBody Map<String, Object> body) {
        if (role != 3) return ApiResponse.forbidden("仅买家可以发起预约");

        Long sellerId = Long.valueOf(body.get("sellerId").toString());
        LocalDate appointmentDate = LocalDate.parse(body.get("appointmentDate").toString());
        String timeSlot = (String) body.get("timeSlot");

        try {
            Appointment appointment = appointmentService.create(userId, sellerId, appointmentDate, timeSlot);
            return ApiResponse.success("预约已提交，等待卖家确认", appointment);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Appointment>> list(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(appointmentService.list(userId, role, status));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            appointmentService.confirm(userId, id);
            return ApiResponse.success("已确认", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("rejectReason");
        try {
            appointmentService.reject(userId, id, reason);
            return ApiResponse.success("已拒绝", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<Void> complete(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            appointmentService.complete(userId, id);
            return ApiResponse.success("已标记完成", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            appointmentService.cancel(userId, id);
            return ApiResponse.success("已取消", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}/breach")
    public ApiResponse<Void> breach(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            appointmentService.breach(userId, id);
            return ApiResponse.success("已标记毁约", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
