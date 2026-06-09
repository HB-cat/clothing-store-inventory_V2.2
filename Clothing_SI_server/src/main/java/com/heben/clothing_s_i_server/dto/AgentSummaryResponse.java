package com.heben.clothing_s_i_server.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 智能体经营简报 — 一次聚合查询返回概要数据
 */
@Data
public class AgentSummaryResponse {

    /** 今日简报 */
    private TodayBrief todayBrief;

    /** 库存预警（最多5条） */
    private List<StockWarning> stockWarnings;

    /** 热销商品（最多5条） */
    private List<HotProduct> hotProducts;

    /** 智能建议 */
    private String suggestion;

    @Data
    public static class TodayBrief {
        private BigDecimal totalAmount;
        private BigDecimal totalProfit;
        private int orderCount;
        private String summary;
    }

    @Data
    public static class StockWarning {
        private String productName;
        private String attrs;
        private int stock;
    }

    @Data
    public static class HotProduct {
        private String productName;
        private int salesCount;
        private BigDecimal salesAmount;
    }
}
