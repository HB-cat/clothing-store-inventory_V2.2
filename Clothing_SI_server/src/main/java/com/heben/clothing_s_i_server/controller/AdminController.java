package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("adminUser") != null) return "redirect:/admin/users";
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(String username, String password, HttpSession session, Model model) {
        SysUser user = adminService.login(username, password);
        if (user == null) {
            model.addAttribute("error", "用户名或密码错误");
            return "admin/login";
        }
        session.setAttribute("adminUser", user);
        return "redirect:/admin/users";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String keyword,
                        HttpSession session, Model model) {
        if (session.getAttribute("adminUser") == null) return "redirect:/admin/login";
        List<SysUser> users = adminService.listUsers(keyword);
        Map<String, Object> stats = adminService.getUserStats();
        model.addAttribute("users", users);
        model.addAttribute("stats", stats);
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    @PostMapping("/users/toggle")
    public String toggleUser(Long userId, HttpSession session) {
        if (session.getAttribute("adminUser") == null) return "redirect:/admin/login";
        adminService.toggleUserStatus(userId);
        return "redirect:/admin/users";
    }

    @GetMapping("/data")
    public String data(HttpSession session, Model model) {
        if (session.getAttribute("adminUser") == null) return "redirect:/admin/login";
        List<Map<String, Object>> rankings = adminService.getDataRanking();
        model.addAttribute("rankings", rankings);
        return "admin/data";
    }

    @GetMapping("/config")
    public String config(HttpSession session) {
        if (session.getAttribute("adminUser") == null) return "redirect:/admin/login";
        return "admin/config";
    }

    @PostMapping("/config/cleanup-appointments")
    @ResponseBody
    public ApiResponse<?> cleanupAppointments(HttpSession session) {
        if (session.getAttribute("adminUser") == null) return ApiResponse.unauthorized("未登录");
        adminService.cleanupAppointments();
        return ApiResponse.success("过期预约数据已清理", null);
    }

    @PostMapping("/config/cleanup-testdata")
    @ResponseBody
    public ApiResponse<?> cleanupTestData(HttpSession session) {
        if (session.getAttribute("adminUser") == null) return ApiResponse.unauthorized("未登录");
        adminService.cleanupTestData();
        return ApiResponse.success("测试数据已清理", null);
    }
}
