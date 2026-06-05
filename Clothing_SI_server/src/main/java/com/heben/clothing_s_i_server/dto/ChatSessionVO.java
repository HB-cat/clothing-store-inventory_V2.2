package com.heben.clothing_s_i_server.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {
    private Long id;
    private Long otherUserId;
    private String otherUserName;    // shopName（商家）或 nickname（买家）
    private Integer otherUserRole;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private LocalDateTime createdAt;
}
