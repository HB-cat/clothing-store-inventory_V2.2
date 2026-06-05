package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.dto.ChatMessageVO;
import com.heben.clothing_s_i_server.dto.ChatSessionVO;
import com.heben.clothing_s_i_server.entity.ChatMessage;
import com.heben.clothing_s_i_server.entity.ChatSession;
import com.heben.clothing_s_i_server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ApiResponse<ChatSession> createSession(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long otherUserId = Long.valueOf(body.get("otherUserId").toString());
        try {
            ChatSession session = chatService.createOrGetSession(userId, otherUserId);
            return ApiResponse.success(session);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionVO>> listSessions(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(chatService.listSessions(userId));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageVO>> getMessages(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ApiResponse.success(chatService.getMessages(userId, id, page, size));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<ChatMessage> sendMessage(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer messageType = body.get("messageType") != null
                ? Integer.parseInt(body.get("messageType").toString()) : 1;
        String content = (String) body.get("content");
        Long refId = body.get("refId") != null
                ? Long.valueOf(body.get("refId").toString()) : null;

        try {
            ChatMessage msg = chatService.sendMessage(userId, id, messageType, content, refId);
            return ApiResponse.success("发送成功", msg);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
