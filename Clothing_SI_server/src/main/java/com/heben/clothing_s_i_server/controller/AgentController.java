package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.AgentParseResponse;
import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/parse")
    public ApiResponse<AgentParseResponse> parse(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {

        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ApiResponse.badRequest("请输入指令内容");
        }

        AgentParseResponse result = agentService.parse(userId, text);
        return ApiResponse.success(result);
    }
}
