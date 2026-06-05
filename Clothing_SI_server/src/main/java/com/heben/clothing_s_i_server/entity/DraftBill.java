package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("draft_bill")
public class DraftBill {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer billIndex;

    private BigDecimal totalAmount;

    private Integer itemCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
