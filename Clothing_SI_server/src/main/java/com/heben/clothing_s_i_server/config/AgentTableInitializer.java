package com.heben.clothing_s_i_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时自动创建智能体消息表（如不存在）
 */
@Slf4j
@Component
public class AgentTableInitializer {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        String sql = """
            CREATE TABLE IF NOT EXISTS agent_message (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL COMMENT '所属用户ID',
                role VARCHAR(10) NOT NULL DEFAULT 'user' COMMENT '角色: user/agent',
                content TEXT COMMENT '消息内容',
                msg_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text/rich',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                INDEX idx_user_created (user_id, created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体聊天消息表'
            """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("✅ agent_message 表已就绪");
        } catch (Exception e) {
            log.warn("⚠️ agent_message 表初始化失败: {}", e.getMessage());
        }
    }
}
