package com.heben.clothing_s_i_server.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerProfile {
    private String buyerName;
    private int orderCount;
    private BigDecimal totalAmount;
    private BigDecimal avgAmount;
    private LocalDateTime lastBuyDate;
}
