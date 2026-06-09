package com.heben.clothing_s_i_server.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class AgentParseResponse {
    /** 意图名称，对应前端 AgentRuleEngine 的 action */
    private String action;
    /** 显示文本，用于前端确认弹窗 */
    private String displayText;
    /** 解析参数，对应前端 ParseResult.params */
    private Map<String, Object> params = new HashMap<>();
    /** 置信度 */
    private double confidence;
    /** 原始输入文本 */
    private String rawText;
    /** 错误信息（可选） */
    private String errorMessage;

    /** 从后端intent/entities转换为前端格式 */
    public static AgentParseResponse fromIntentAndEntities(String intent, Map<String, Object> entities, double confidence, String rawText) {
        AgentParseResponse resp = new AgentParseResponse();
        resp.action = mapIntentToAction(intent);
        resp.confidence = confidence;
        resp.rawText = rawText;
        resp.params = entities != null ? entities : new HashMap<>();
        resp.displayText = buildDisplayText(resp.action, entities);
        return resp;
    }

    private static String mapIntentToAction(String intent) {
        if (intent == null) return "unknown";
        switch (intent) {
            case "quick_sale": return "sale";
            case "add_product": return "add_product";
            case "add_category": return "add_category";
            case "reorder_category": return "module_sort";
            case "add_attr_value": return "open_page";
            case "purchase_stock": return "purchase";
            case "query_sales": return "query_sales";
            case "return_product": return "return";
            case "open_page": return "open_page";
            default: return "unknown";
        }
    }

    private static String buildDisplayText(String action, Map<String, Object> entities) {
        if (entities == null) return "已识别指令";
        switch (action) {
            case "sale":
                return "📝 销售记账\n商品: " + entities.getOrDefault("productName", "") + "\n确认记账吗？";
            case "add_product":
                return "📦 添加商品\n名称: " + entities.getOrDefault("name", "") + "\n进价: " + entities.getOrDefault("purchasePrice", "") + " 售价: " + entities.getOrDefault("sellingPrice", "") + "\n确认添加？";
            case "add_category":
                return "📂 添加分类: " + entities.getOrDefault("name", "") + "\n确认添加？";
            case "purchase":
                return "📦 进货入库\n商品: " + entities.getOrDefault("productName", "") + " 数量: " + entities.getOrDefault("quantity", "") + " 进价: " + entities.getOrDefault("unitPrice", "") + "\n确认入库？";
            case "query_sales":
                return "📊 查询" + entities.getOrDefault("period", "") + "销售数据\n为您查询中...";
            case "return":
                return "↩️ 退货: " + entities.getOrDefault("productName", "") + "\n确认退货？";
            case "open_page":
                return "🔗 跳转到: " + entities.getOrDefault("pageName", "");
            case "module_sort":
                return "🔀 移动「" + entities.getOrDefault("name", "") + "」到第" + entities.getOrDefault("targetPosition", "") + "位\n确认调整？";
            default:
                return "已识别指令";
        }
    }
}
