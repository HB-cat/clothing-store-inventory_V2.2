package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wholesale_transaction")
public class WholesaleTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wholesalerId;

    private Long retailerId;

    private Long productId;

    private Long combinationId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;
}
