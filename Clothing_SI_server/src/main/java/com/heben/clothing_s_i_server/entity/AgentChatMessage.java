package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体聊天消息 — 独立于B2B聊天，持久化智能体对话
 */
@Data
@TableName("agent_message")
public class AgentChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 角色: user / agent */
    private String role;

    /** 消息内容 */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String content;

    /** 消息类型: text=文本, rich=富文本 */
    private String msgType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
