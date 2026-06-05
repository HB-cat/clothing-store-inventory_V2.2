package com.heben.clothing_s_i_server.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TodayReport {
    private BigDecimal todaySales;
    private BigDecimal todayProfit;
    private int todayOrders;
    private int todayCustomers;
    private String summaryMessage;
}
