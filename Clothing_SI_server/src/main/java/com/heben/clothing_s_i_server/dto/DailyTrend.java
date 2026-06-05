package com.heben.clothing_s_i_server.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyTrend {
    private LocalDate date;
    private BigDecimal totalAmount;
    private int orderCount;
}
