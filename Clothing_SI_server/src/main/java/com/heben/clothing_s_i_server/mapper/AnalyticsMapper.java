package com.heben.clothing_s_i_server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsMapper {

    /** 今日快报 */
    @Select("SELECT " +
            "  COALESCE(SUM(total_amount), 0) AS todaySales, " +
            "  COALESCE(SUM(total_profit), 0) AS todayProfit, " +
            "  COUNT(*) AS todayOrders, " +
            "  COUNT(DISTINCT buyer_name) AS todayCustomers " +
            "FROM sale_record " +
            "WHERE user_id = #{userId} AND sale_time >= CURDATE()")
    Map<String, Object> selectTodayReport(Long userId);

    /** 滞销提醒（30天有库存无销量） */
    @Select("SELECT p.id AS productId, p.name AS productName, " +
            "  pac.id AS combinationId, pac.attrs_json AS attrsDesc, " +
            "  pac.stock AS stock, " +
            "  DATEDIFF(CURDATE(), COALESCE(MAX(sri.created_at), '2000-01-01')) AS daysWithoutSale " +
            "FROM product p " +
            "  JOIN product_attr_combination pac ON p.id = pac.product_id " +
            "  LEFT JOIN sale_record_item sri ON pac.id = sri.combination_id AND sri.is_returned = 0 " +
            "WHERE p.user_id = #{userId} AND p.is_deleted = 0 AND pac.stock > 0 " +
            "GROUP BY pac.id " +
            "HAVING daysWithoutSale >= 30 " +
            "ORDER BY daysWithoutSale DESC")
    List<Map<String, Object>> selectSlowMoving(Long userId);

    /** 补货建议（库存≤5） */
    @Select("SELECT pac.id AS combinationId, p.id AS productId, " +
            "  p.name AS productName, pac.attrs_json AS attrsDesc, pac.stock AS stock " +
            "FROM product p " +
            "  JOIN product_attr_combination pac ON p.id = pac.product_id " +
            "WHERE p.user_id = #{userId} AND p.is_deleted = 0 AND pac.stock > 0 AND pac.stock <= 5 " +
            "ORDER BY pac.stock ASC")
    List<Map<String, Object>> selectLowStock(Long userId);

    /** 热销TOP8 */
    @Select("SELECT sri.product_id AS productId, MAX(sri.product_name) AS productName, " +
            "  SUM(sri.quantity) AS totalQuantity, SUM(sri.unit_price * sri.quantity) AS totalAmount " +
            "FROM sale_record_item sri " +
            "  JOIN sale_record sr ON sri.sale_id = sr.id " +
            "WHERE sr.user_id = #{userId} AND sri.is_returned = 0 " +
            "GROUP BY sri.product_id " +
            "ORDER BY totalQuantity DESC LIMIT 8")
    List<Map<String, Object>> selectTop8(Long userId);

    /** 近7天趋势 */
    @Select("SELECT DATE(sr.sale_time) AS date, " +
            "  COALESCE(SUM(sr.total_amount), 0) AS totalAmount, COUNT(*) AS orderCount " +
            "FROM sale_record sr " +
            "WHERE sr.user_id = #{userId} AND sr.sale_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(sr.sale_time) ORDER BY date")
    List<Map<String, Object>> selectTrend7Days(Long userId);

    /** 订单高峰24小时分布 */
    @Select("SELECT HOUR(sr.sale_time) AS hour, COUNT(*) AS orderCount " +
            "FROM sale_record sr " +
            "WHERE sr.user_id = #{userId} " +
            "GROUP BY HOUR(sr.sale_time) ORDER BY hour")
    List<Map<String, Object>> selectPeakHours(Long userId);

    /** 客户画像 */
    @Select("SELECT sr.buyer_name AS buyerName, COUNT(*) AS orderCount, " +
            "  COALESCE(SUM(sr.total_amount), 0) AS totalAmount, " +
            "  COALESCE(AVG(sr.total_amount), 0) AS avgAmount, " +
            "  MAX(sr.sale_time) AS lastBuyDate " +
            "FROM sale_record sr " +
            "WHERE sr.user_id = #{userId} AND sr.buyer_id IS NOT NULL AND sr.buyer_name != '散客' " +
            "GROUP BY sr.buyer_name ORDER BY totalAmount DESC")
    List<Map<String, Object>> selectCustomerProfile(Long userId);
}
