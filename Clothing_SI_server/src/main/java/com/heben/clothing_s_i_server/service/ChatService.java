package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heben.clothing_s_i_server.dto.ChatMessageVO;
import com.heben.clothing_s_i_server.dto.ChatSessionVO;
import com.heben.clothing_s_i_server.entity.ChatMessage;
import com.heben.clothing_s_i_server.entity.ChatSession;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.mapper.ChatMessageMapper;
import com.heben.clothing_s_i_server.mapper.ChatSessionMapper;
import com.heben.clothing_s_i_server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SysUserMapper sysUserMapper;

    @Transactional
    public ChatSession createOrGetSession(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) throw new IllegalArgumentException("不能和自己聊天");

        Long a = Math.min(userId, otherUserId);
        Long b = Math.max(userId, otherUserId);

        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getParticipantA, a)
                        .eq(ChatSession::getParticipantB, b)
        );
        if (session != null) return session;

        session = new ChatSession();
        session.setParticipantA(a);
        session.setParticipantB(b);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    public List<ChatSessionVO> listSessions(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .and(w -> w.eq(ChatSession::getParticipantA, userId)
                                .or().eq(ChatSession::getParticipantB, userId))
                        .orderByDesc(ChatSession::getLastMessageAt)
        );

        List<ChatSessionVO> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());

            Long otherId = s.getParticipantA().equals(userId) ? s.getParticipantB() : s.getParticipantA();
            SysUser other = sysUserMapper.selectById(otherId);

            vo.setOtherUserId(otherId);
            if (other != null) {
                String displayName = (other.getShopName() != null) ? other.getShopName() : other.getNickname();
                vo.setOtherUserName(displayName != null ? displayName : other.getUsername());
                vo.setOtherUserRole(other.getRole());
            }

            vo.setLastMessage(s.getLastMessage());
            vo.setLastMessageAt(s.getLastMessageAt());

            boolean isA = s.getParticipantA().equals(userId);
            vo.setUnreadCount(isA ? s.getUnreadA() : s.getUnreadB());

            vo.setCreatedAt(s.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    public List<ChatMessageVO> getMessages(Long userId, Long sessionId, int page, int size) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");
        if (!session.getParticipantA().equals(userId) && !session.getParticipantB().equals(userId)) {
            throw new IllegalArgumentException("无权查看该会话");
        }

        Page<ChatMessage> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreatedAt);

        Page<ChatMessage> result = chatMessageMapper.selectPage(pageObj, wrapper);

        List<ChatMessageVO> vos = result.getRecords().stream().map(m -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(m.getId());
            vo.setSessionId(m.getSessionId());
            vo.setSenderId(m.getSenderId());
            vo.setMessageType(m.getMessageType());
            vo.setContent(m.getContent());
            vo.setRefId(m.getRefId());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        if (page == 1) clearUnread(userId, session);

        java.util.Collections.reverse(vos);
        return vos;
    }

    @Transactional
    public ChatMessage sendMessage(Long userId, Long sessionId, Integer messageType, String content, Long refId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");
        if (!session.getParticipantA().equals(userId) && !session.getParticipantB().equals(userId)) {
            throw new IllegalArgumentException("无权在该会话发送消息");
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderId(userId);
        msg.setMessageType(messageType != null ? messageType : 1);
        msg.setContent(content);
        msg.setRefId(refId);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);

        String summary = content;
        if (summary != null && summary.length() > 100) summary = summary.substring(0, 100);
        session.setLastMessage(summary);
        session.setLastMessageAt(LocalDateTime.now());

        if (session.getParticipantA().equals(userId)) {
            session.setUnreadB(session.getUnreadB() == null ? 1 : session.getUnreadB() + 1);
        } else {
            session.setUnreadA(session.getUnreadA() == null ? 1 : session.getUnreadA() + 1);
        }

        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        return msg;
    }

    private void clearUnread(Long userId, ChatSession session) {
        boolean isA = session.getParticipantA().equals(userId);
        if (isA && (session.getUnreadA() != null && session.getUnreadA() > 0)) {
            session.setUnreadA(0);
            chatSessionMapper.updateById(session);
        } else if (!isA && (session.getUnreadB() != null && session.getUnreadB() > 0)) {
            session.setUnreadB(0);
            chatSessionMapper.updateById(session);
        }
    }
}
