package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shared_product_pool")
public class SharedProductPool {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wholesalerId;

    private Long productId;

    private BigDecimal sharedPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
