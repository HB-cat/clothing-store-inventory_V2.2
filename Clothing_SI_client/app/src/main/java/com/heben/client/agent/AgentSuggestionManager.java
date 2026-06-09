package com.heben.client.agent;

import android.content.Context;
import android.content.SharedPreferences;

import com.heben.client.util.AppContext;
import com.heben.client.util.TokenManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 主动建议管理器 — 学习用户行为，在适当时机主动推荐操作
 * 存储操作频次、常用商品、页面停留时长、查询历史
 */
public class AgentSuggestionManager {

    private static final String PREF_NAME = "agent_learning";
    private static final String KEY_OPEN_COUNT = "open_count";
    private static final String KEY_UNSUBMITTED_BILLS = "unsubmitted_bills";
    private static final String KEY_LAST_HOME_TIME = "last_home_time";
    private static final String KEY_SUGGEST_DISABLED = "suggest_disabled";
    private static final String KEY_SALE_COUNT = "sale_count";
    private static final String KEY_PURCHASE_COUNT = "purchase_count";
    private static final String KEY_TOP_PRODUCTS = "top_products";
    private static final String KEY_ACTION_FREQ_PREFIX = "action_freq_";
    private static final String KEY_PAGE_VISIT_PREFIX = "page_visit_";
    private static final String KEY_STOCK_ALERTS = "stock_alerts";
    private static final String KEY_LAST_SUGGEST_TIME = "last_suggest_time";
    private static final String KEY_TOTAL_ACTIONS = "total_actions";

    // ===== Phase C: 个性化学习 =====
    private static final String KEY_SALE_PRICE_PREFIX = "sale_price_";
    private static final String KEY_LAST_PURCHASE_PREFIX = "last_purchase_";
    private static final String KEY_PURCHASE_TIMES_PREFIX = "purchase_times_";
    private static final String KEY_PRICE_RECORD_COUNT_PREFIX = "price_count_";

    public static class Suggestion {
        public String text;
        public String action;
        public int priority; // 0=low, 1=medium, 2=high

        public Suggestion(String text, String action) {
            this.text = text;
            this.action = action;
            this.priority = 1;
        }

        public Suggestion(String text, String action, int priority) {
            this.text = text;
            this.action = action;
            this.priority = priority;
        }
    }

    private static SharedPreferences getPrefs() {
        return AppContext.get().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 记录用户打开首页 */
    public static void recordHomeOpen() {
        SharedPreferences sp = getPrefs();
        sp.edit()
                .putInt(KEY_OPEN_COUNT, getHomeOpenCount() + 1)
                .putLong(KEY_LAST_HOME_TIME, System.currentTimeMillis())
                .putInt(KEY_TOTAL_ACTIONS, sp.getInt(KEY_TOTAL_ACTIONS, 0) + 1)
                .apply();
    }

    public static int getHomeOpenCount() {
        return getPrefs().getInt(KEY_OPEN_COUNT, 0);
    }

    /** 记录未提交账单数量 */
    public static void setUnsubmittedBills(int count) {
        getPrefs().edit().putInt(KEY_UNSUBMITTED_BILLS, count).apply();
    }

    public static int getUnsubmittedBills() {
        return getPrefs().getInt(KEY_UNSUBMITTED_BILLS, 0);
    }

    /** 记录销售次数 */
    public static void recordSale() {
        SharedPreferences sp = getPrefs();
        sp.edit()
                .putInt(KEY_SALE_COUNT, sp.getInt(KEY_SALE_COUNT, 0) + 1)
                .putInt(KEY_TOTAL_ACTIONS, sp.getInt(KEY_TOTAL_ACTIONS, 0) + 1)
                .apply();
        recordAction("sale");
    }

    public static int getSaleCount() {
        return getPrefs().getInt(KEY_SALE_COUNT, 0);
    }

    /** 记录进货次数 */
    public static void recordPurchase() {
        SharedPreferences sp = getPrefs();
        sp.edit()
                .putInt(KEY_PURCHASE_COUNT, sp.getInt(KEY_PURCHASE_COUNT, 0) + 1)
                .putInt(KEY_TOTAL_ACTIONS, sp.getInt(KEY_TOTAL_ACTIONS, 0) + 1)
                .apply();
        recordAction("purchase");
    }

    public static int getPurchaseCount() {
        return getPrefs().getInt(KEY_PURCHASE_COUNT, 0);
    }

    /** 记录查询行为 */
    public static void recordQuery(String type) {
        String key = KEY_ACTION_FREQ_PREFIX + "query_" + type;
        SharedPreferences sp = getPrefs();
        sp.edit()
                .putInt(key, sp.getInt(key, 0) + 1)
                .putInt(KEY_TOTAL_ACTIONS, sp.getInt(KEY_TOTAL_ACTIONS, 0) + 1)
                .apply();
    }

    /** 记录任意操作行为 */
    public static void recordAction(String action) {
        String key = KEY_ACTION_FREQ_PREFIX + action;
        SharedPreferences sp = getPrefs();
        sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply();
    }

    /** 获取某操作频次 */
    public static int getActionFreq(String action) {
        return getPrefs().getInt(KEY_ACTION_FREQ_PREFIX + action, 0);
    }

    /** 记录库存预警数量 */
    public static void setStockAlerts(int count) {
        getPrefs().edit().putInt(KEY_STOCK_ALERTS, count).apply();
    }

    public static int getStockAlerts() {
        return getPrefs().getInt(KEY_STOCK_ALERTS, 0);
    }

    /** 记录页面访问 */
    public static void recordPageVisit(String pageName) {
        String key = KEY_PAGE_VISIT_PREFIX + pageName;
        SharedPreferences sp = getPrefs();
        sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply();
    }

    /** 获取操作次数排名前N的行为 */
    public static List<String> getTopActions(int n) {
        Map<Integer, String> sorted = new TreeMap<>((a, b) -> b - a);
        SharedPreferences sp = getPrefs();
        Map<String, ?> all = sp.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith(KEY_ACTION_FREQ_PREFIX) && entry.getValue() instanceof Integer) {
                sorted.put((Integer) entry.getValue(), entry.getKey().substring(KEY_ACTION_FREQ_PREFIX.length()));
            }
        }
        List<String> result = new ArrayList<>();
        for (String action : sorted.values()) {
            result.add(action);
            if (result.size() >= n) break;
        }
        return result;
    }

    /** 是否关闭了主动建议 */
    public static boolean isSuggestDisabled() {
        return getPrefs().getBoolean(KEY_SUGGEST_DISABLED, false);
    }

    public static void setSuggestDisabled(boolean disabled) {
        getPrefs().edit().putBoolean(KEY_SUGGEST_DISABLED, disabled).apply();
    }

    /** 距上次建议是否已过冷却期（默认5分钟） */
    private static boolean isCooldownPassed() {
        long lastTime = getPrefs().getLong(KEY_LAST_SUGGEST_TIME, 0);
        return System.currentTimeMillis() - lastTime > 5 * 60 * 1000;
    }

    /** 生成主动建议列表 — 按优先级排序 */
    public static List<Suggestion> getSuggestions() {
        List<Suggestion> list = new ArrayList<>();
        if (isSuggestDisabled() || !TokenManager.isLoggedIn()) return list;
        if (!isCooldownPassed()) return list; // 冷却期内不重复建议

        SharedPreferences sp = getPrefs();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int totalActions = sp.getInt(KEY_TOTAL_ACTIONS, 0);

        // ===== 高优先级 =====

        // 1. 有未提交账单（最高优先级）
        int bills = getUnsubmittedBills();
        if (bills > 0) {
            list.add(new Suggestion("您有 " + bills + " 个未提交账单，需要提交吗？", "submit_bills", 2));
        }

        // 2. 库存预警
        int stockAlerts = getStockAlerts();
        if (stockAlerts > 0) {
            String productWord = stockAlerts <= 3 ? "商品" : "商品";
            list.add(new Suggestion("⚠️ 有 " + stockAlerts + " 件" + productWord + "库存不足，说「查库存」查看详情", "check_stock", 2));
        }

        // 3. 刚完成记账 → 推荐新建账单
        if (getSaleCount() > 0 && getSaleCount() % 3 == 0) {
            list.add(new Suggestion("您已记账 " + getSaleCount() + " 次，试试新建一个正式账单？", "new_bill", 2));
        }

        // 3. 刚完成进货 → 推荐查看库存
        if (getPurchaseCount() > 0 && getPurchaseCount() % 2 == 0) {
            list.add(new Suggestion("已进货 " + getPurchaseCount() + " 次，查看一下当前库存？", "check_stock", 2));
        }

        // ===== 中优先级 =====

        // 4. 高峰时段推荐查账
        if (hour >= 20 && hour <= 22) {
            list.add(new Suggestion("打烊时间到了，查看今天的销售情况？", "query_sales", 1));
        } else if (hour >= 8 && hour <= 10) {
            list.add(new Suggestion("早上好！查看昨天的销售情况？", "query_sales", 1));
        } else if (hour >= 14 && hour <= 16) {
            list.add(new Suggestion("下午好，趁着空闲记一下上午的账？", "quick_sale", 1));
        }

        // 5. 根据使用频率推荐
        int opens = getHomeOpenCount();
        if (opens >= 3 && opens % 5 == 0) {
            list.add(new Suggestion("使用熟练了！试试说「卖了2件白T恤」快速记账", "learn_quick", 1));
        }

        // 6. 学习用户高频操作
        List<String> topActions = getTopActions(2);
        if (!topActions.isEmpty()) {
            String freqAction = topActions.get(0);
            if ("stock_query".equals(freqAction) || "query_sales".equals(freqAction)) {
                list.add(new Suggestion("您经常查询数据，说「查分析」一键查看经营报表", "analytics_hint", 1));
            } else if ("sale".equals(freqAction)) {
                list.add(new Suggestion("您最常用记账功能，说「卖了3件白T恤」快捷开单", "sale_hint", 1));
            }
        }

        // ===== 低优先级 =====

        // 7. 库存查看提示
        if (opens > 0 && opens % 10 == 0) {
            list.add(new Suggestion("说「有哪些库存不足的商品」查看缺货情况", "stock_hint", 0));
        }

        // 8. 新手引导
        if (totalActions < 5) {
            list.add(new Suggestion("我是您的智能助手，说「帮助」查看我能做什么", "help_hint", 0));
        }

        // 9. 常用商品推荐
        List<String> topProducts = getTopProducts();
        if (!topProducts.isEmpty() && totalActions > 10 && opens % 7 == 0) {
            String top = topProducts.get(0);
            list.add(new Suggestion("「" + top + "」是您最常卖的商品，继续补货吗？", "restock_hint", 0));
        }

        // ===== Phase C: 个性化学习建议 =====

        // 10. 根据售价记录，推荐定价参考
        if (!topProducts.isEmpty()) {
            String top = topProducts.get(0);
            double avgPrice = getAvgSalePrice(top);
            int priceCount = getSalePriceCount(top);
            if (avgPrice > 0 && priceCount >= 2) {
                list.add(new Suggestion("💡 「" + top + "」您通常卖 ¥" + String.format("%.0f", avgPrice) + "/件，按这个价继续卖？", "price_hint", 1));
            }
        }

        // 11. 根据进货周期，提醒补货
        if (!topProducts.isEmpty()) {
            for (String product : topProducts.subList(0, Math.min(topProducts.size(), 3))) {
                int daysSince = getDaysSinceLastPurchase(product);
                int purchaseTimes = getPurchaseProductCount(product);
                if (daysSince > 0 && purchaseTimes >= 2 && daysSince >= 7) {
                    list.add(new Suggestion("⚠️ 距上次进「" + product + "」已过 " + daysSince + " 天，是否需要补货？", "restock_hint", 1));
                    break; // 只提醒一个商品
                }
            }
        }

        // 12. 学习习惯：频繁进购+查询的用户推荐经营分析
        if (getSaleCount() >= 5 && getPurchaseCount() >= 3) {
            list.add(new Suggestion("📊 您已记了 " + getSaleCount() + " 笔账、进了 " + getPurchaseCount() + " 次货，查看经营分析？", "analytics_hint", 1));
        }

        // 更新建议时间
        if (!list.isEmpty()) {
            sp.edit().putLong(KEY_LAST_SUGGEST_TIME, System.currentTimeMillis()).apply();
        }

        return list;
    }

    /** 记录常用商品（最多10个） */
    public static void recordProductUsed(String productName) {
        if (productName == null || productName.trim().isEmpty()) return;
        String raw = getPrefs().getString(KEY_TOP_PRODUCTS, "");
        List<String> products = new ArrayList<>();
        if (!raw.isEmpty()) {
            String[] arr = raw.split(",");
            for (String s : arr) {
                if (!s.isEmpty()) products.add(s);
            }
        }
        products.remove(productName);
        products.add(0, productName);
        if (products.size() > 10) products = products.subList(0, 10);
        StringBuilder sb = new StringBuilder();
        for (String s : products) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        getPrefs().edit().putString(KEY_TOP_PRODUCTS, sb.toString()).apply();
    }

    public static List<String> getTopProducts() {
        String raw = getPrefs().getString(KEY_TOP_PRODUCTS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : raw.split(",")) {
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    // ===== Phase C: 学习定价 =====

    /** 记录某商品的售价，用于计算建议售价 */
    public static void recordSalePrice(String productName, double price) {
        if (productName == null || productName.trim().isEmpty() || price <= 0) return;
        SharedPreferences sp = getPrefs();
        String keySum = KEY_SALE_PRICE_PREFIX + productName;
        String keyCount = KEY_PRICE_RECORD_COUNT_PREFIX + productName;
        double sum = sp.getFloat(keySum, 0f) + (float) price;
        int count = sp.getInt(keyCount, 0) + 1;
        sp.edit()
                .putFloat(keySum, (float) sum)
                .putInt(keyCount, count)
                .apply();
    }

    /** 获取某商品的历史平均售价（0表示无记录） */
    public static double getAvgSalePrice(String productName) {
        if (productName == null) return 0;
        SharedPreferences sp = getPrefs();
        double sum = sp.getFloat(KEY_SALE_PRICE_PREFIX + productName, 0f);
        int count = sp.getInt(KEY_PRICE_RECORD_COUNT_PREFIX + productName, 0);
        return count > 0 ? sum / count : 0;
    }

    /** 获取某商品的售价记录次数 */
    public static int getSalePriceCount(String productName) {
        return getPrefs().getInt(KEY_PRICE_RECORD_COUNT_PREFIX + productName, 0);
    }

    // ===== Phase C: 学习进货周期 =====

    /** 记录进货时间 */
    public static void recordPurchaseCycle(String productName) {
        if (productName == null || productName.trim().isEmpty()) return;
        SharedPreferences sp = getPrefs();
        String keyLast = KEY_LAST_PURCHASE_PREFIX + productName;
        String keyTimes = KEY_PURCHASE_TIMES_PREFIX + productName;
        long now = System.currentTimeMillis();
        sp.edit()
                .putLong(keyLast, now)
                .putInt(keyTimes, sp.getInt(keyTimes, 0) + 1)
                .apply();
    }

    /** 获取距上次进货该商品的天数（-1表示无记录） */
    public static int getDaysSinceLastPurchase(String productName) {
        long lastTime = getPrefs().getLong(KEY_LAST_PURCHASE_PREFIX + productName, 0);
        if (lastTime == 0) return -1;
        long diff = System.currentTimeMillis() - lastTime;
        return (int) (diff / (24 * 60 * 60 * 1000));
    }

    /** 获取某商品的进货次数 */
    public static int getPurchaseProductCount(String productName) {
        return getPrefs().getInt(KEY_PURCHASE_TIMES_PREFIX + productName, 0);
    }
}
