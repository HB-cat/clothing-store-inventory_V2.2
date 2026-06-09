package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heben.clothing_s_i_server.entity.AgentChatMessage;
import com.heben.clothing_s_i_server.mapper.AgentChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体聊天记录服务 — 持久化智能体对话
 * 小窗（AgentBottomSheetDialog）和全屏（AgentPanelActivity）共享同一份数据
 */
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final AgentChatMessageMapper mapper;

    /** 获取用户的智能体聊天记录（按时间升序） */
    public List<AgentChatMessage> getMessages(Long userId, int page, int size) {
        Page<AgentChatMessage> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<AgentChatMessage> wrapper = new LambdaQueryWrapper<AgentChatMessage>()
                .eq(AgentChatMessage::getUserId, userId)
                .orderByDesc(AgentChatMessage::getCreatedAt);
        Page<AgentChatMessage> result = mapper.selectPage(pageObj, wrapper);
        List<AgentChatMessage> list = result.getRecords();
        // 反转成时间升序（最早的在前）
        java.util.Collections.reverse(list);
        return list;
    }

    /** 保存一条消息 */
    public AgentChatMessage saveMessage(Long userId, String role, String content, String msgType) {
        AgentChatMessage msg = new AgentChatMessage();
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMsgType(msgType != null ? msgType : "text");
        msg.setCreatedAt(LocalDateTime.now());
        mapper.insert(msg);
        return msg;
    }

    /** 保存用户消息 + 智能体回复（批量） */
    public void saveExchange(Long userId, String userContent, String agentContent) {
        saveMessage(userId, "user", userContent, "text");
        saveMessage(userId, "agent", agentContent, "text");
    }

    /** 删除用户的所有智能体聊天记录 */
    public void clearMessages(Long userId) {
        mapper.delete(new LambdaQueryWrapper<AgentChatMessage>()
                .eq(AgentChatMessage::getUserId, userId));
    }
}
