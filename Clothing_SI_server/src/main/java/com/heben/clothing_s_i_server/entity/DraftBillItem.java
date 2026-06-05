package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("draft_bill_item")
public class DraftBillItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;

    private Long productId;

    private Long combinationId;

    private String attrsJson;

    private Integer quantity;

    private BigDecimal unitPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
