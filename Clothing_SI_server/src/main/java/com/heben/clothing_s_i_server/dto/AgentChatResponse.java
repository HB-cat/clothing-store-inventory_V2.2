package com.heben.clothing_s_i_server.dto;

import lombok.Data;

/**
 * 智能体问答响应 — DeepSeek 基于业务数据的自然语言回答
 */
@Data
public class AgentChatResponse {
    /** AI 的回答文本 */
    private String reply;
    /** 回答类型: analysis(数据分析), knowledge(知识问答), suggestion(经营建议) */
    private String replyType;
    /** 是否需要前端展示数据卡片 */
    private boolean showDataCard;
    /** 结构化数据（可选，前端可渲染为卡片） */
    private Object dataCard;
}
