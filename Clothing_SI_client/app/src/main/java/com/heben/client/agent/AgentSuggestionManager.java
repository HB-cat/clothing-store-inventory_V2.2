package com.heben.client.agent;

import android.content.Context;
import android.content.SharedPreferences;

import com.heben.client.util.AppContext;
import com.heben.client.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 主动建议管理器 — 学习用户行为，在适当时机主动推荐操作
 * 存储操作频次、常用商品、页面停留时长
 */
public class AgentSuggestionManager {

    private static final String PREF_NAME = "agent_learning";
    private static final String KEY_OPEN_COUNT = "open_count";
    private static final String KEY_UNSUBMITTED_BILLS = "unsubmitted_bills";
    private static final String KEY_LAST_HOME_TIME = "last_home_time";
    private static final String KEY_SUGGEST_DISABLED = "suggest_disabled";

    public static class Suggestion {
        public String text;
        public String action;

        public Suggestion(String text, String action) {
            this.text = text;
            this.action = action;
        }
    }

    private static SharedPreferences getPrefs() {
        return AppContext.get().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 记录用户打开首页 */
    public static void recordHomeOpen() {
        getPrefs().edit()
                .putInt(KEY_OPEN_COUNT, getHomeOpenCount() + 1)
                .putLong(KEY_LAST_HOME_TIME, System.currentTimeMillis())
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

    /** 是否关闭了主动建议 */
    public static boolean isSuggestDisabled() {
        return getPrefs().getBoolean(KEY_SUGGEST_DISABLED, false);
    }

    public static void setSuggestDisabled(boolean disabled) {
        getPrefs().edit().putBoolean(KEY_SUGGEST_DISABLED, disabled).apply();
    }

    /** 生成主动建议列表 */
    public static List<Suggestion> getSuggestions() {
        List<Suggestion> list = new ArrayList<>();
        if (isSuggestDisabled() || !TokenManager.isLoggedIn()) return list;

        // 1. 有未提交账单
        int bills = getUnsubmittedBills();
        if (bills > 0) {
            list.add(new Suggestion("您有 " + bills + " 个未提交账单，需要提交吗？", "submit_bills"));
        }

        // 2. 根据打开次数学习——使用超过3次后推送
        int opens = getHomeOpenCount();
        if (opens >= 3 && opens % 5 == 0) {
            list.add(new Suggestion("使用熟练了！试试说「卖了2件白T恤」快速记账", "learn_quick"));
        }

        // 3. 高峰时段推荐查账
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 20 && hour <= 22) {
            list.add(new Suggestion("打烊时间到了，查看今天的销售情况？", "query_sales"));
        } else if (hour >= 8 && hour <= 10) {
            list.add(new Suggestion("早上好！查看昨天的销售情况？", "query_sales"));
        }

        // 4. 随机提示
        if (opens > 0 && opens % 10 == 0) {
            list.add(new Suggestion("说「有哪些库存不足的商品」查看缺货情况", "stock_hint"));
        }

        return list;
    }

    /** 记录常用商品（最多10个） */
    private static final String KEY_TOP_PRODUCTS = "top_products";

    public static void recordProductUsed(String productName) {
        String raw = getPrefs().getString(KEY_TOP_PRODUCTS, "");
        List<String> products = new ArrayList<>();
        if (!raw.isEmpty()) {
            String[] arr = raw.split(",");
            for (String s : arr) products.add(s);
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
}
