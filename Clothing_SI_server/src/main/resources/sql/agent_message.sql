-- 智能体聊天消息表
-- 用于持久化智能体对话，支持小窗/全屏共享聊天记录
-- 请手动执行此SQL，或在application.properties中添加 spring.sql.init.mode=always

CREATE TABLE IF NOT EXISTS agent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    role VARCHAR(10) NOT NULL DEFAULT 'user' COMMENT '角色: user/agent',
    content TEXT COMMENT '消息内容',
    msg_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text/rich',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体聊天消息表';
