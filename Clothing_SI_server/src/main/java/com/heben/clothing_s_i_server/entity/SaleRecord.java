package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sale_record")
public class SaleRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long buyerId;

    private String buyerName;

    private BigDecimal totalAmount;

    private Integer itemCount;

    private Long draftBillId;

    private LocalDateTime saleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<SaleRecordItem> saleItems;
}
