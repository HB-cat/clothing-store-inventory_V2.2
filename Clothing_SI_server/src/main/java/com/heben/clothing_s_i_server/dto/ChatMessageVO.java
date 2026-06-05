package com.heben.clothing_s_i_server.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Integer messageType;
    private String content;
    private Long refId;
    private LocalDateTime createdAt;
}
