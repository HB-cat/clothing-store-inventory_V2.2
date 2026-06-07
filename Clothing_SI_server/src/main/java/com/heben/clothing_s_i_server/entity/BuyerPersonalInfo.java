package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("buyer_info")
public class BuyerPersonalInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer age;

    private Integer height;

    private BigDecimal weight;

    private BigDecimal bust;

    private BigDecimal waist;

    private BigDecimal hip;

    private String stylePreference;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
