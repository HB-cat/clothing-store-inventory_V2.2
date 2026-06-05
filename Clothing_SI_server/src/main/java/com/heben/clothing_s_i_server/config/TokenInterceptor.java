package com.heben.clothing_s_i_server.config;

import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 拦截器：自动从请求头提取 Token，验证后将用户信息注入请求属性
 */
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未提供Token\",\"data\":null}");
            return false;
        }

        // 提取 Bearer token
        String token = authHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        SysUser user = authService.validateToken(token);
        if (user == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            return false;
        }

        // 将用户ID和角色注入请求属性，Controller 中通过 @RequestAttribute 获取
        request.setAttribute("currentUserId", user.getId());
        request.setAttribute("currentUserRole", user.getRole());
        return true;
    }
}
