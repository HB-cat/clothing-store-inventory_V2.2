package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_record")
public class PurchaseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer source;

    private BigDecimal totalAmount;

    private String supplierName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
