package com.heben.clothing_s_i_server.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.heben.clothing_s_i_server.dto.AgentChatResponse;
import com.heben.clothing_s_i_server.dto.AgentParseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final RestTemplate restTemplate;
    private final AnalyticsService analyticsService;
    private final Gson gson = new Gson();

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
你是一个服装进销存系统的智能助手。收到用户的自然语言指令后，解析为结构化 JSON。

返回格式严格如下，不要多余内容：
{
  "intent": "意图名称",
  "entities": { 键值对参数 },
  "confidence": 0.0~1.0
}

=== 支持的意图和参数 ===

1. quick_sale — 快捷记账
   参数: productName, attrs(可选), quantity, unitPrice, buyerName(可选，默认"散客")
   示例: "红色M码连衣裙1件150元，李女士"
   → {"intent":"quick_sale","entities":{"productName":"连衣裙","attrs":"红色 M码","quantity":1,"unitPrice":150,"buyerName":"李女士"}}

2. add_product — 添加商品
   参数: name, purchasePrice, sellingPrice, categoryName(可选)
   示例: "添加商品'牛仔裤'，进价80，售价150"
   → {"intent":"add_product","entities":{"name":"牛仔裤","purchasePrice":80,"sellingPrice":150}}

3. add_category — 添加分类
   参数: name
   示例: "添加分类'鞋类'"
   → {"intent":"add_category","entities":{"name":"鞋类"}}

4. reorder_category — 分类排序
   参数: name, targetPosition(数字，从1开始)
   示例: "把'裤子类'移到第一位"
   → {"intent":"reorder_category","entities":{"name":"裤子类","targetPosition":1}}

5. add_attr_value — 添加属性值
   参数: groupName, value
   示例: "添加颜色属性值'卡其色'"
   → {"intent":"add_attr_value","entities":{"groupName":"颜色","value":"卡其色"}}

6. purchase_stock — 进货
   参数: productName, quantity, unitPrice, attrs(可选)
   示例: "进货牛仔裤10条，进价70元"
   → {"intent":"purchase_stock","entities":{"productName":"牛仔裤","quantity":10,"unitPrice":70}}

7. query_sales — 查询销售
   参数: period(today/week/month), timeRange(可选)
   示例: "查询本月销售额"
   → {"intent":"query_sales","entities":{"period":"month"}}

8. return_product — 退货
   参数: productName(可选), quantity(可选)
   示例: "退掉刚才那笔连衣裙"
   → {"intent":"return_product","entities":{"productName":"连衣裙"}}

9. open_page — 打开页面
   参数: pageName
   示例: "打开经营分析"
   → {"intent":"open_page","entities":{"pageName":"经营分析"}}

10. unknown — 无法识别的指令
    参数: rawText
    注意：如果用户问与系统功能无关的问题（如"谁给你提供的技术支持"、"你的开发者是谁"、"你是什么"等），也应返回 unknown
    → {"intent":"unknown","entities":{"rawText":"..."},"confidence":0}

=== 规则 ===
- 数量、价格等数字填数值类型，不要加引号
- 无法解析时返回 confidence=0 的 unknown
- 商品名称精确提取，去掉"商品"、"的"等干扰词
""";

    /**
     * 智能问答 System Prompt — 用于 chat 接口
     * AI 作为服装店分析助手，基于系统提供的真实业务数据回答问题
     */
    private static final String CHAT_SYSTEM_PROMPT = """
你是一个服装店老板的智能分析助手。系统会为你提供店铺的真实经营数据，你需要基于数据回答用户的问题。

回答要求：
1. **基于数据**：引用系统提供的数据来支撑你的分析，不要编造数据
2. **结论先行**：先给出核心结论，再展开说明
3. **简单易懂**：用自然的口语表达，像助手在跟老板汇报
4. **给出建议**：分析后给出可操作的建议
5. **简洁明了**：不超过200字，分段清晰

回答风格示例：
"对比上月，本月销售额增长了15%，从¥12,000涨到了¥13,800。连衣裙是增长主力，多卖了10件。建议趁热打铁，增加连衣裙的陈列位置。"

如果用户问的是与店铺经营无关的问题（天气、百科等），直接用你的知识回答，风格保持亲切友好。

注意：不要使用Markdown格式，用纯文本+emoji即可。
""";

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 2;

    public AgentParseResponse parse(Long userId, String text) {
        AgentParseResponse response = new AgentParseResponse();
        response.setRawText(text);

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> requestBody = buildDeepSeekRequest(text);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> rawResponse = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, request, Map.class
                );

                if (rawResponse.getBody() == null) {
                    if (attempt < MAX_RETRIES) continue;
                    return fallbackUnknown(text, "DeepSeek 返回空响应");
                }

                String content = extractContent(rawResponse.getBody());
                if (content == null) {
                    if (attempt < MAX_RETRIES) continue;
                    return fallbackUnknown(text, "无法解析 DeepSeek 返回格式");
                }

                String jsonStr = extractJson(content);
                Map<String, Object> result = gson.fromJson(jsonStr,
                        new TypeToken<Map<String, Object>>() {}.getType());

                String intent = (String) result.getOrDefault("intent", "unknown");
                @SuppressWarnings("unchecked")
                Map<String, Object> entities = result.get("entities") instanceof Map
                        ? (Map<String, Object>) result.get("entities") : null;
                Object confidenceObj = result.get("confidence");
                double confidence = confidenceObj instanceof Number
                        ? ((Number) confidenceObj).doubleValue() : 0.5;

                return AgentParseResponse.fromIntentAndEntities(intent, entities, confidence, text);

            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000L * (attempt + 1)); // 指数退避: 1s, 2s
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "未知错误";
        if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
            return fallbackUnknown(text, "DeepSeek API 请求超时，已重试" + MAX_RETRIES + "次，请稍后重试");
        } else if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
            return fallbackUnknown(text, "DeepSeek API 认证失败，请检查 API Key 配置");
        } else {
            return fallbackUnknown(text, "DeepSeek API 调用失败：" + errorMsg);
        }
    }

    private Map<String, Object> buildDeepSeekRequest(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.1);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", text));
        body.put("messages", messages);

        return body;
    }

    private String extractContent(Map<String, Object> responseBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractJson(String content) {
        content = content.trim();
        if (content.startsWith("```")) {
            int start = content.indexOf('\n');
            int end = content.lastIndexOf("```");
            if (start > 0 && end > start) {
                content = content.substring(start, end).trim();
            }
        }
        if (content.startsWith("json")) {
            content = content.substring(4).trim();
        }
        return content;
    }

    private AgentParseResponse fallbackUnknown(String text, String errorMessage) {
        return AgentParseResponse.fromIntentAndEntities("unknown", Map.of("rawText", text), 0.0, text);
    }

    // ===== Phase B: 智能问答 =====

    /** 需要业务数据的关键词 */
    private static final Set<String> DATA_KEYWORDS = Set.of(
            "销售", "生意", "卖", "赚", "亏", "利润", "毛利", "收入", "营业额",
            "对比", "比较", "趋势", "分析", "涨", "跌", "增长", "下降",
            "库存", "缺货", "补货", "进货", "热销", "畅销", "滞销",
            "客户", "顾客", "预约", "订单"
    );

    /**
     * 智能问答 — 自动判断是否需要业务数据，带上下文调 DeepSeek
     */
    public AgentChatResponse chat(Long userId, String text) {
        AgentChatResponse resp = new AgentChatResponse();
        boolean needsData = needsBusinessData(text);

        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append("用户问题：").append(text).append("\n\n");

        if (needsData) {
            // 获取业务数据上下文
            String context = buildBusinessContext(userId);
            fullPrompt.append("=== 以下是你店铺的真实经营数据 ===\n");
            fullPrompt.append(context).append("\n\n");
            fullPrompt.append("请基于以上数据回答用户的问题。如果数据不足以回答，请说明并给出基于现有数据的分析。");
        } else {
            fullPrompt.append("请直接回答用户的问题，保持友好亲切的语气。");
        }

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> requestBody = buildChatRequest(fullPrompt.toString());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> rawResponse = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, request, Map.class
                );

                if (rawResponse.getBody() == null) {
                    if (attempt < MAX_RETRIES) continue;
                    return fallbackChat(text, "网络开小差了，请稍后重试");
                }

                String content = extractContent(rawResponse.getBody());
                if (content == null) {
                    if (attempt < MAX_RETRIES) continue;
                    return fallbackChat(text, "AI 暂时无法回答，请换个问法试试");
                }

                resp.setReply(content.trim());
                resp.setReplyType(needsData ? "analysis" : "knowledge");
                resp.setShowDataCard(needsData);
                return resp;

            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000L * (attempt + 1));
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "";
        if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
            return fallbackChat(text, "⏰ AI 思考超时了，问题太复杂的话可以拆开问哦");
        } else if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
            return fallbackChat(text, "🔑 AI 服务认证失败，请联系管理员检查 API Key 配置");
        } else {
            return fallbackChat(text, "📡 AI 服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 判断是否需要查询业务数据
     */
    private boolean needsBusinessData(String text) {
        String lower = text.toLowerCase();
        return DATA_KEYWORDS.stream().anyMatch(kw -> lower.contains(kw));
    }

    /**
     * 构建业务数据上下文
     */
    private String buildBusinessContext(Long userId) {
        StringBuilder ctx = new StringBuilder();

        try {
            // 今日简报
            var today = analyticsService.getTodayReport(userId);
            ctx.append("【今日简报】\n");
            ctx.append("销售额: ¥").append(today.getTodaySales()).append("\n");
            ctx.append("毛利: ¥").append(today.getTodayProfit()).append("\n");
            ctx.append("订单数: ").append(today.getTodayOrders()).append(" 单\n");
            ctx.append("结论: ").append(today.getSummaryMessage()).append("\n\n");

            // 热销TOP5
            var topProducts = analyticsService.getTop8(userId);
            if (!topProducts.isEmpty()) {
                ctx.append("【热销商品 TOP5】\n");
                int limit = Math.min(topProducts.size(), 5);
                for (int i = 0; i < limit; i++) {
                    var p = topProducts.get(i);
                    ctx.append(i + 1).append(". ").append(p.getProductName())
                            .append(" 销量:").append(p.getTotalQuantity())
                            .append(" 金额:¥").append(p.getTotalAmount()).append("\n");
                }
                ctx.append("\n");
            }

            // 库存预警
            var lowStock = analyticsService.getLowStock(userId);
            if (!lowStock.isEmpty()) {
                ctx.append("【库存预警】\n");
                for (var p : lowStock) {
                    ctx.append("- ").append(p.getProductName());
                    if (p.getAttrsDesc() != null && !p.getAttrsDesc().isEmpty()) {
                        ctx.append("(").append(p.getAttrsDesc()).append(")");
                    }
                    ctx.append(" 仅剩 ").append(p.getStock()).append(" 件\n");
                }
                ctx.append("\n");
            }

            // 近7天趋势
            var trend = analyticsService.getTrend7Days(userId);
            if (!trend.isEmpty()) {
                ctx.append("【近7天销售趋势】\n");
                for (var d : trend) {
                    ctx.append("- ").append(d.getDate()).append(": ¥").append(d.getTotalAmount()).append("\n");
                }
                ctx.append("\n");
            }

            // 常客画像 TOP5
            var customers = analyticsService.getCustomerProfile(userId);
            if (!customers.isEmpty()) {
                ctx.append("【常客画像 TOP5】\n");
                int limit = Math.min(customers.size(), 5);
                for (int i = 0; i < limit; i++) {
                    var c = customers.get(i);
                    ctx.append(i + 1).append(". ").append(c.getBuyerName())
                            .append(" 消费:").append(c.getOrderCount()).append("次")
                            .append(" 总额:¥").append(c.getTotalAmount()).append("\n");
                }
                ctx.append("\n");
            }

            // 滞销商品提醒
            var slowMoving = analyticsService.getSlowMoving(userId);
            if (!slowMoving.isEmpty()) {
                ctx.append("【滞销商品提醒】\n");
                int limit = Math.min(slowMoving.size(), 3);
                for (int i = 0; i < limit; i++) {
                    var p = slowMoving.get(i);
                    ctx.append("- ").append(p.getProductName())
                            .append(" 已 ").append(p.getDaysWithoutSale()).append(" 天未售出")
                            .append(" 库存:").append(p.getStock()).append("件\n");
                }
                ctx.append("\n");
            }

        } catch (Exception e) {
            ctx.append("（暂未获取到经营数据）\n");
        }

        return ctx.toString();
    }

    /**
     * 构建问答请求
     */
    private Map<String, Object> buildChatRequest(String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7); // 聊天场景稍高的温度，更自然

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", CHAT_SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userMessage));
        body.put("messages", messages);

        return body;
    }

    /**
     * 问答失败降级
     */
    private AgentChatResponse fallbackChat(String text, String message) {
        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(message);
        resp.setReplyType("knowledge");
        resp.setShowDataCard(false);
        return resp;
    }
}
