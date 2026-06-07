package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buyer_crm")
public class BuyerInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sellerId;

    private Long buyerUserId;

    private String buyerName;

    private String phone;

    private String remark;

    private Integer visitCount;

    private LocalDateTime lastVisitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
