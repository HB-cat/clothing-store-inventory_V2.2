package com.heben.clothing_s_i_server.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
    → {"intent":"unknown","entities":{"rawText":"..."},"confidence":0}

=== 规则 ===
- 数量、价格等数字填数值类型，不要加引号
- 无法解析时返回 confidence=0 的 unknown
- 商品名称精确提取，去掉"商品"、"的"等干扰词
""";

    public AgentParseResponse parse(Long userId, String text) {
        AgentParseResponse response = new AgentParseResponse();
        response.setRawText(text);

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
                return fallbackUnknown(text, "DeepSeek 返回空响应");
            }

            String content = extractContent(rawResponse.getBody());
            if (content == null) {
                return fallbackUnknown(text, "无法解析 DeepSeek 返回格式");
            }

            String jsonStr = extractJson(content);
            Map<String, Object> result = gson.fromJson(jsonStr,
                    new TypeToken<Map<String, Object>>() {}.getType());

            response.setIntent((String) result.getOrDefault("intent", "unknown"));
            Object entitiesObj = result.get("entities");
            if (entitiesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entities = (Map<String, Object>) entitiesObj;
                response.setEntities(entities);
            }
            Object confidenceObj = result.get("confidence");
            if (confidenceObj instanceof Number) {
                response.setConfidence(((Number) confidenceObj).doubleValue());
            } else {
                response.setConfidence(0.5);
            }

            return response;

        } catch (Exception e) {
            return fallbackUnknown(text, "解析失败：" + e.getMessage());
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
        AgentParseResponse resp = new AgentParseResponse();
        resp.setIntent("unknown");
        resp.setEntities(Map.of("rawText", text));
        resp.setRawText(text);
        resp.setConfidence(0.0);
        resp.setErrorMessage(errorMessage);
        return resp;
    }
}
