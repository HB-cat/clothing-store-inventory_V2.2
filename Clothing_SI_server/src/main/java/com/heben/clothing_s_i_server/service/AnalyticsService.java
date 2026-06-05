package com.heben.clothing_s_i_server.service;

import com.heben.clothing_s_i_server.dto.*;
import com.heben.clothing_s_i_server.mapper.AnalyticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsMapper analyticsMapper;

    public TodayReport getTodayReport(Long userId) {
        Map<String, Object> raw = analyticsMapper.selectTodayReport(userId);
        TodayReport report = new TodayReport();
        report.setTodaySales(toBigDecimal(raw.get("todaySales")));
        report.setTodayProfit(toBigDecimal(raw.get("todayProfit")));
        report.setTodayOrders(toInt(raw.get("todayOrders")));
        report.setTodayCustomers(toInt(raw.get("todayCustomers")));
        report.setSummaryMessage(generateSummary(report));
        return report;
    }

    public List<SlowMovingProduct> getSlowMoving(Long userId) {
        return analyticsMapper.selectSlowMoving(userId).stream().map(r -> {
            SlowMovingProduct p = new SlowMovingProduct();
            p.setProductId(toLong(r.get("productId")));
            p.setProductName((String) r.get("productName"));
            p.setCombinationId(toLong(r.get("combinationId")));
            p.setAttrsDesc((String) r.get("attrsDesc"));
            p.setStock(toInt(r.get("stock")));
            p.setDaysWithoutSale(toInt(r.get("daysWithoutSale")));
            return p;
        }).collect(Collectors.toList());
    }

    public List<LowStockProduct> getLowStock(Long userId) {
        return analyticsMapper.selectLowStock(userId).stream().map(r -> {
            LowStockProduct p = new LowStockProduct();
            p.setCombinationId(toLong(r.get("combinationId")));
            p.setProductId(toLong(r.get("productId")));
            p.setProductName((String) r.get("productName"));
            p.setAttrsDesc((String) r.get("attrsDesc"));
            p.setStock(toInt(r.get("stock")));
            return p;
        }).collect(Collectors.toList());
    }

    public List<TopProduct> getTop8(Long userId) {
        return analyticsMapper.selectTop8(userId).stream().map(r -> {
            TopProduct p = new TopProduct();
            p.setProductId(toLong(r.get("productId")));
            p.setProductName((String) r.get("productName"));
            p.setTotalQuantity(toInt(r.get("totalQuantity")));
            p.setTotalAmount(toBigDecimal(r.get("totalAmount")));
            return p;
        }).collect(Collectors.toList());
    }

    public List<DailyTrend> getTrend7Days(Long userId) {
        return analyticsMapper.selectTrend7Days(userId).stream().map(r -> {
            DailyTrend t = new DailyTrend();
            Object dateObj = r.get("date");
            if (dateObj instanceof java.sql.Date) {
                t.setDate(((java.sql.Date) dateObj).toLocalDate());
            } else if (dateObj instanceof LocalDate) {
                t.setDate((LocalDate) dateObj);
            }
            t.setTotalAmount(toBigDecimal(r.get("totalAmount")));
            t.setOrderCount(toInt(r.get("orderCount")));
            return t;
        }).collect(Collectors.toList());
    }

    public List<PeakHour> getPeakHours(Long userId) {
        return analyticsMapper.selectPeakHours(userId).stream().map(r -> {
            PeakHour p = new PeakHour();
            p.setHour(toInt(r.get("hour")));
            p.setOrderCount(toInt(r.get("orderCount")));
            return p;
        }).collect(Collectors.toList());
    }

    public List<CustomerProfile> getCustomerProfile(Long userId) {
        return analyticsMapper.selectCustomerProfile(userId).stream().map(r -> {
            CustomerProfile p = new CustomerProfile();
            p.setBuyerName((String) r.get("buyerName"));
            p.setOrderCount(toInt(r.get("orderCount")));
            p.setTotalAmount(toBigDecimal(r.get("totalAmount")));
            p.setAvgAmount(toBigDecimal(r.get("avgAmount")));
            Object dateObj = r.get("lastBuyDate");
            if (dateObj instanceof java.sql.Timestamp) {
                p.setLastBuyDate(((java.sql.Timestamp) dateObj).toLocalDateTime());
            } else if (dateObj instanceof LocalDateTime) {
                p.setLastBuyDate((LocalDateTime) dateObj);
            }
            return p;
        }).collect(Collectors.toList());
    }

    private String generateSummary(TodayReport report) {
        BigDecimal sales = report.getTodaySales();
        BigDecimal profit = report.getTodayProfit();
        int orders = report.getTodayOrders();
        int customers = report.getTodayCustomers();

        if (orders == 0) {
            return "今天还没有开张，可以试试推广引流～";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("今天共 ").append(orders).append(" 单");

        if (customers > 0) {
            sb.append("，").append(customers).append(" 位顾客");
        }

        sb.append("，销售额 ").append(sales.setScale(2, BigDecimal.ROUND_HALF_UP)).append(" 元");

        if (profit.compareTo(BigDecimal.ZERO) > 0) {
            double profitRate = profit.divide(sales, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            sb.append("，毛利 ").append(profit.setScale(2, BigDecimal.ROUND_HALF_UP)).append(" 元");
            sb.append("（毛利率 ").append(String.format("%.1f", profitRate)).append("%）");
        }

        if (sales.compareTo(new BigDecimal("5000")) > 0) {
            sb.append("，生意不错！🔥");
        } else if (sales.compareTo(new BigDecimal("1000")) > 0) {
            sb.append("，表现平稳。");
        } else {
            sb.append("，还需努力💪");
        }

        return sb.toString();
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof BigDecimal) return ((BigDecimal) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Double) return BigDecimal.valueOf((Double) val);
        if (val instanceof Integer) return BigDecimal.valueOf((Integer) val);
        if (val instanceof Long) return BigDecimal.valueOf((Long) val);
        return new BigDecimal(val.toString());
    }
}
