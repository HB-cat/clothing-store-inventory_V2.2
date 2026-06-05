package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_setting")
public class NotificationSetting {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer agentSuggestion;

    private Integer appointmentRequest;

    private Integer appointmentResult;

    private Integer arrivalReminder;

    private Integer chatMessage;

    private Integer wholesaleRequest;

    private Integer wholesaleConfirm;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
