package com.heben.client.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地规则引擎 — 正则匹配 + 关键词提取
 * 覆盖16种指令类型，≤200ms，无需网络
 * 规则引擎无法匹配的指令转发 DeepSeek
 */
public class AgentRuleEngine {

    public static class ParseResult {
        public String action;          // 指令类型标识
        public double confidence;      // 置信度 0-1
        public String displayText;     // 展示文本
        public java.util.HashMap<String, Object> params; // 提取的参数

        public ParseResult(String action, double confidence, String displayText) {
            this.action = action;
            this.confidence = confidence;
            this.displayText = displayText;
            this.params = new java.util.HashMap<>();
        }
    }

    // ===== 指令规则定义 =====
    private static final Rule[] RULES = new Rule[]{
        // 1. 销售记账: "卖了2件红色M码连衣裙，每件90元"
        new Rule("sale", 0.95,
                "卖了(.+?)(\\d+)件.*?每件(\\d+(?:\\.\\d+)?)元",
                "sales_quick"),

        // 2. 销售记账(简写): "卖了一条牛仔裤80元"
        new Rule("sale", 0.90,
                "卖了(.+?)(\\d+(?:\\.\\d+)?)元",
                "sales_quick_simple"),

        // 3. 新建账单: "新建一个账单，加入T恤3件"
        new Rule("new_bill", 0.95,
                "新建.*?账单.*?加入(.+?)(\\d+)件",
                "draft_create"),

        // 4. 提交账单: "提交第一个账单" / "提交账单1"
        new Rule("submit_bill", 0.95,
                "提交(?:第?)(\\d+).*?账单",
                "draft_submit"),

        // 5. 查询销售: "本月销售额" / "今天毛利多少" / "本月销售"
        new Rule("query_sales", 0.90,
                "(今天|今日|本月|本周|本年|上月).*?(销售额|毛利|销售|利润|收入)",
                "sales_query"),

        // 6. 退货: "退掉刚才那笔连衣裙" / "退掉连衣裙"
        new Rule("return", 0.85,
                "退掉\\s*(.+?)$",
                "sales_return"),

        // 7. 接受预约: "接受李小姐的预约" / "接受预约"
        new Rule("accept_appointment", 0.90,
                "接受\\s*(.+?)的预约",
                "appointment_accept"),

        // 8. 拒绝预约: "拒绝王先生的预约，理由今天休息"
        new Rule("reject_appointment", 0.90,
                "拒绝\\s*(.+?)的预约[，,]+理由\\s*(.+)",
                "appointment_reject"),

        // 9. 打开页面: "打开经营分析" / "进入商品管理" / "打开我的页面"
        new Rule("open_page", 0.95,
                "(?:打开|进入|跳转)\\s*(.+)$",
                "page_navigate"),

        // 10. 添加分类: "添加分类'鞋类'" / "添加分类鞋类"
        new Rule("add_category", 0.95,
                "添加分类['\"\\s]*(.+?)['\"\\s]*$",
                "category_add"),

        // 11. 删除分类: "删除'上衣'分类" / "删除分类上衣"
        new Rule("delete_category", 0.90,
                "删除\\s*(?:分类)?['\"\\s]*(.+?)['\"\\s]*(?:分类)?$",
                "category_delete"),

        // 12. 添加商品: "添加商品'牛仔裤'，进价80，售价150"
        new Rule("add_product", 0.90,
                "添加商品['\"\\s]*(.+?)['\"\\s]*[，,]+进价\\s*(\\d+)[，,]+售价\\s*(\\d+)",
                "product_add"),

        // 13. 进货入库: "进货牛仔裤10条，进价70元"
        new Rule("purchase", 0.85,
                "进货\\s*(.+?)\\s*(\\d+)条?[，,]+进价\\s*(\\d+)",
                "purchase_in"),

        // 14. 库存查询: "有哪些库存不足的商品？" / "库存不足"
        new Rule("stock_query", 0.90,
                "库存不足|缺货|库存低于|哪些商品.*?库存",
                "stock_low"),

        // 15. 系统设置: "关闭声音" / "打开通知"
        new Rule("system_setting", 0.95,
                "(关闭|打开|开启)\\s*(声音|通知|音效|提示音|振动)",
                "setting_toggle"),

        // 16. 模块排序: "把商品管理移到第一位"
        new Rule("module_sort", 0.85,
                "把\\s*(.+?)\\s*移[到至]\\s*(?:第?)(\\d+)\\s*位",
                "module_reorder"),

        // 17. 发商品卡片: "把连衣裙发给李小姐"
        new Rule("send_card", 0.80,
                "把\\s*(.+?)\\s*发给\\s*(.+)",
                "product_card_send"),
    };

    /** 匹配输入文本，返回最佳匹配结果 */
    public static ParseResult parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParseResult("unknown", 0, "请输入指令");
        }

        String text = input.trim();

        // 遍历规则，找最高置信度匹配
        ParseResult best = null;
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern.matcher(text);
            if (matcher.find()) {
                ParseResult result = new ParseResult(rule.action, rule.confidence, buildDisplayText(rule, matcher));
                // 提取参数
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    result.params.put("arg" + i, matcher.group(i));
                }
                if (best == null || rule.confidence > best.confidence) {
                    best = result;
                }
            }
        }

        if (best != null) {
            // 如果有matchedType信息
            return best;
        }

        // 未匹配
        return new ParseResult("unknown", 0, "没理解这条指令，换个说法试试？");
    }

    private static String buildDisplayText(Rule rule, Matcher m) {
        switch (rule.action) {
            case "sale":
                String product = m.group(1);
                return "📝 销售记账\n商品: " + product + "\n确认记账吗？";
            case "new_bill":
                return "📝 新建账单\n加入商品: " + m.group(1) + " × " + m.group(2) + "\n确认创建？";
            case "submit_bill":
                return "📝 提交第" + m.group(1) + "个账单\n确认提交？";
            case "query_sales":
                return "📊 查询" + m.group(1) + m.group(2) + "\n为您查询中...";
            case "return":
                return "↩️ 退货: " + m.group(1) + "\n确认退货？";
            case "accept_appointment":
                return "✅ 接受" + m.group(1) + "的预约\n确认接受？";
            case "reject_appointment":
                return "❌ 拒绝" + m.group(1) + "的预约\n理由: " + m.group(2) + "\n确认拒绝？";
            case "open_page":
                return "🔗 跳转到: " + m.group(1);
            case "add_category":
                return "📂 添加分类: " + m.group(1) + "\n确认添加？";
            case "delete_category":
                return "🗑️ 删除分类: " + m.group(1) + "\n确认删除？";
            case "add_product":
                return "📦 添加商品\n名称: " + m.group(1) + "\n进价: " + m.group(2) + " 售价: " + m.group(3) + "\n确认添加？";
            case "purchase":
                return "📦 进货入库\n商品: " + m.group(1) + " 数量: " + m.group(2) + " 进价: " + m.group(3) + "\n确认入库？";
            case "stock_query":
                return "📋 查询库存不足商品\n正在为您查询...";
            case "system_setting":
                String action = m.group(1);
                String target = m.group(2);
                return "⚙️ " + (action.contains("关") ? "关闭" : "打开") + target + "\n确认设置？";
            case "module_sort":
                return "🔀 移动「" + m.group(1) + "」到第" + m.group(2) + "位\n确认调整？";
            case "send_card":
                return "📤 将「" + m.group(1) + "」发送给" + m.group(2) + "\n确认发送？";
            default:
                return "已识别指令: " + rule.action;
        }
    }

    /** 规则结构体 */
    private static class Rule {
        final String action;
        final double confidence;
        final Pattern pattern;
        final String matchedType;

        Rule(String action, double confidence, String regex, String matchedType) {
            this.action = action;
            this.confidence = confidence;
            this.pattern = Pattern.compile(regex);
            this.matchedType = matchedType;
        }
    }
}
