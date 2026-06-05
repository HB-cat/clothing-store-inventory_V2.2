package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private Integer role;

    private String shopName;

    private String nickname;

    private String avatarUrl;

    private String token;

    private LocalDateTime tokenExpire;

    private Integer status;

    private LocalDateTime appointmentBanUntil;

    private Integer appointmentBanCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
