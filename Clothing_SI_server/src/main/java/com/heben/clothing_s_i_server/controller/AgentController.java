package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.AgentChatResponse;
import com.heben.clothing_s_i_server.dto.AgentParseResponse;
import com.heben.clothing_s_i_server.dto.AgentSummaryResponse;
import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.AgentChatMessage;
import com.heben.clothing_s_i_server.service.AgentChatService;
import com.heben.clothing_s_i_server.service.AgentService;
import com.heben.clothing_s_i_server.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AnalyticsService analyticsService;
    private final AgentChatService agentChatService;

    @PostMapping("/parse")
    public ApiResponse<AgentParseResponse> parse(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {

        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ApiResponse.badRequest("请输入指令内容");
        }

        AgentParseResponse result = agentService.parse(userId, text);
        return ApiResponse.success(result);
    }

    /**
     * 智能体经营简报 — 聚合今日概览、库存预警、热销商品
     */
    @GetMapping("/summary")
    public ApiResponse<AgentSummaryResponse> getSummary(
            @RequestAttribute("currentUserId") Long userId) {

        AgentSummaryResponse resp = new AgentSummaryResponse();

        // 1. 今日简报
        var todayReport = analyticsService.getTodayReport(userId);
        AgentSummaryResponse.TodayBrief brief = new AgentSummaryResponse.TodayBrief();
        brief.setTotalAmount(todayReport.getTodaySales());
        brief.setTotalProfit(todayReport.getTodayProfit());
        brief.setOrderCount(todayReport.getTodayOrders());
        brief.setSummary(todayReport.getSummaryMessage());
        resp.setTodayBrief(brief);

        // 2. 库存预警（取前5笔）
        List<AgentSummaryResponse.StockWarning> warnings = analyticsService.getLowStock(userId).stream()
                .limit(5)
                .map(p -> {
                    AgentSummaryResponse.StockWarning w = new AgentSummaryResponse.StockWarning();
                    w.setProductName(p.getProductName());
                    w.setAttrs(p.getAttrsDesc());
                    w.setStock(p.getStock());
                    return w;
                })
                .collect(Collectors.toList());
        resp.setStockWarnings(warnings);

        // 3. 热销商品（取前5笔）
        List<AgentSummaryResponse.HotProduct> hotProducts = analyticsService.getTop8(userId).stream()
                .limit(5)
                .map(p -> {
                    AgentSummaryResponse.HotProduct h = new AgentSummaryResponse.HotProduct();
                    h.setProductName(p.getProductName());
                    h.setSalesCount(p.getTotalQuantity());
                    h.setSalesAmount(p.getTotalAmount());
                    return h;
                })
                .collect(Collectors.toList());
        resp.setHotProducts(hotProducts);

        // 4. 智能建议
        resp.setSuggestion(generateSuggestion(brief, warnings));

        return ApiResponse.success(resp);
    }

    /**
     * 智能体问答 — 基于业务数据的 AI 分析问答
     * 自动保存用户问题和AI回复到聊天记录
     */
    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {

        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ApiResponse.badRequest("请输入问题");
        }

        AgentChatResponse result = agentService.chat(userId, text);

        // 保存用户消息 + AI回复（确保AI已回复才保存）
        agentChatService.saveExchange(userId, text, result.getReply());

        return ApiResponse.success(result);
    }

    /**
     * 获取智能体聊天历史（分页，时间升序）
     */
    @GetMapping("/messages")
    public ApiResponse<List<AgentChatMessage>> getMessages(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(agentChatService.getMessages(userId, page, size));
    }

    /**
     * 清空智能体聊天记录
     */
    @DeleteMapping("/messages")
    public ApiResponse<Void> clearMessages(
            @RequestAttribute("currentUserId") Long userId) {
        agentChatService.clearMessages(userId);
        return ApiResponse.success("已清空", null);
    }

    /**
     * 保存一条用户消息+智能体回复（本地规则引擎匹配时调用）
     * 前端传入 { userMessage, agentMessage }，后端将两条记录写入 DB
     */
    @PostMapping("/messages/save-pair")
    public ApiResponse<Void> saveMessagePair(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {
        String userMsg = body.get("userMessage");
        String agentMsg = body.get("agentMessage");
        if (userMsg == null || agentMsg == null) {
            return ApiResponse.badRequest("userMessage 和 agentMessage 不能为空");
        }
        agentChatService.saveExchange(userId, userMsg, agentMsg);
        return ApiResponse.success(null);
    }

    private String generateSuggestion(AgentSummaryResponse.TodayBrief brief,
                                       List<AgentSummaryResponse.StockWarning> warnings) {
        StringBuilder sb = new StringBuilder();

        if (brief.getOrderCount() == 0) {
            sb.append("今天还没有开张，可以试试推广引流。");
        } else {
            BigDecimal sales = brief.getTotalAmount();
            if (sales.compareTo(new BigDecimal("5000")) > 0) {
                sb.append("今天生意不错！继续保持🔥");
            } else if (sales.compareTo(new BigDecimal("1000")) > 0) {
                sb.append("今天表现平稳。");
            } else {
                sb.append("今天生意一般，可以看看哪些商品需要推广。");
            }
        }

        if (!warnings.isEmpty()) {
            sb.append(" 有 ").append(warnings.size()).append(" 件商品库存不足，建议及时补货。");
        }

        return sb.toString();
    }
}
