package com.heben.clothing_s_i_server.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopProduct {
    private Long productId;
    private String productName;
    private int totalQuantity;
    private BigDecimal totalAmount;
}
