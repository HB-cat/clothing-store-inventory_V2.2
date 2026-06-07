package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buyer_follow")
public class BuyerFollow {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buyerId;

    private Long sellerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
