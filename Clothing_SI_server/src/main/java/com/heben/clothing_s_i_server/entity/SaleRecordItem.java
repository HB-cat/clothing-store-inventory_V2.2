package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sale_record_item")
public class SaleRecordItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long saleId;

    private Long productId;

    private String productName;

    private Long combinationId;

    private String attrsJson;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal purchasePrice;

    private Integer isReturned;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
