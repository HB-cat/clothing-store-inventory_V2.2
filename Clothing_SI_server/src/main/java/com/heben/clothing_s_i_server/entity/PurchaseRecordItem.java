package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_record_item")
public class PurchaseRecordItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long purchaseId;

    private Long productId;

    private Long combinationId;

    private Integer quantity;

    private BigDecimal unitPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
