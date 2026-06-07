package com.heben.client.agent;

import android.content.Context;

import com.heben.client.agent.AgentRuleEngine.ParseResult;
import com.heben.client.local.entity.LocalPurchaseRecordItem;
import com.heben.client.local.entity.LocalSaleRecordItem;
import com.heben.client.local.sync.OfflineHelper;
import com.heben.client.util.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 伪智能体执行器 — 离线时直接执行本地规则引擎的解析结果
 * 支持：快捷记账、进货入库 — 直接落Room，网络恢复后同步
 * 支持：页面跳转、设置
 * 不支持需联网的操作（分类CRUD、提交草稿等）→ 返回提示信息
 */
public class OfflineAgentExecutor {

    private final Context context;
    private final OfflineHelper offlineHelper;

    public OfflineAgentExecutor(Context context) {
        this.context = context.getApplicationContext();
        this.offlineHelper = new OfflineHelper(this.context);
    }

    /**
     * 执行本地指令
     * @return 执行结果描述（用于Agent气泡显示）
     */
    public ExecuteResult execute(ParseResult result) {
        Long userId = TokenManager.getUserId();
        if (userId == null) {
            return new ExecuteResult(false, "未登录，请先登录");
        }

        switch (result.action) {
            case "sale":
                return executeSale(userId, result);
            case "purchase":
                return executePurchase(userId, result);
            case "open_page":
            case "stock_query":
            case "query_sales":
            case "system_setting":
                return new ExecuteResult(true, result.displayText + "\n正在跳转...");
            default:
                return new ExecuteResult(false, "「" + result.displayText + "」\n此功能需要联网才能使用，请连接网络后重试");
        }
    }

    /** 执行离线记账 */
    private ExecuteResult executeSale(long userId, ParseResult result) {
        String productName;
        int quantity;
        double unitPrice;

        if (result.params.size() >= 3) {
            // "卖了2件M码连衣裙，每件90元" → arg1=商品, arg2=数量, arg3=单价
            productName = String.valueOf(result.params.get("arg1"));
            quantity = Integer.parseInt(String.valueOf(result.params.get("arg2")));
            unitPrice = Double.parseDouble(String.valueOf(result.params.get("arg3")));
        } else if (result.params.size() >= 2) {
            // "卖了一条牛仔裤80元" → arg1=商品, arg2=总价 → 数量默认为1
            productName = String.valueOf(result.params.get("arg1"));
            quantity = 1;
            unitPrice = Double.parseDouble(String.valueOf(result.params.get("arg2")));
        } else {
            return new ExecuteResult(false, "无法解析商品信息，请按格式输入\n例如：卖了2件白T恤，每件59元");
        }

        double totalAmount = quantity * unitPrice;
        String saleTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()).format(new Date());

        List<LocalSaleRecordItem> items = new ArrayList<>();
        LocalSaleRecordItem item = new LocalSaleRecordItem();
        item.productName = productName;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        items.add(item);

        offlineHelper.saveSaleOffline(userId, "", totalAmount, quantity, saleTime, items);

        String msg = "✅ 离线记账成功\n"
                + productName + " × " + quantity + "件\n"
                + "金额: ¥" + String.format("%.2f", totalAmount) + "\n"
                + "网络恢复后自动同步到服务器";
        return new ExecuteResult(true, msg);
    }

    /** 执行离线进货入库 */
    private ExecuteResult executePurchase(long userId, ParseResult result) {
        String productName;
        int quantity;
        double unitPrice;

        // "进货牛仔裤10条，进价70元" → arg1=商品, arg2=数量, arg3=进价
        if (result.params.size() >= 3) {
            productName = String.valueOf(result.params.get("arg1"));
            quantity = Integer.parseInt(String.valueOf(result.params.get("arg2")));
            unitPrice = Double.parseDouble(String.valueOf(result.params.get("arg3")));
        } else {
            return new ExecuteResult(false, "无法解析进货信息，请按格式输入\n例如：进货牛仔裤10条，进价70元");
        }

        double totalAmount = quantity * unitPrice;
        List<LocalPurchaseRecordItem> items = new ArrayList<>();
        LocalPurchaseRecordItem pitem = new LocalPurchaseRecordItem();
        pitem.quantity = quantity;
        pitem.unitPrice = unitPrice;
        items.add(pitem);

        offlineHelper.savePurchaseOffline(userId, 1, totalAmount, "", items);

        String msg = "✅ 离线入库成功\n"
                + productName + " × " + quantity + "\n"
                + "金额: ¥" + String.format("%.2f", totalAmount) + "\n"
                + "网络恢复后自动同步到服务器";
        return new ExecuteResult(true, msg);
    }

    /** 执行结果 */
    public static class ExecuteResult {
        public final boolean success;
        public final String message;

        public ExecuteResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
