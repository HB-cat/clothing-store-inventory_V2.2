package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.agent.AgentRichCardHelper;
import com.heben.client.agent.AgentRuleEngine;
import com.heben.client.agent.AgentSuggestionManager;
import com.heben.client.agent.OfflineAgentExecutor;
import com.heben.client.agent.OfflineAgentExecutor.ExecuteResult;
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.AgentChatMessage;
import com.heben.client.model.Models.AgentChatResponse;
import com.heben.client.model.Models.AgentSummaryResponse;
import com.heben.client.model.Models.LowStockProduct;
import com.heben.client.network.ApiClient;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 智能体全屏聊天页面
 * 与 AgentBottomSheetDialog 共享同一份聊天历史（后端 agent_message 表）
 * 打开时自动加载历史，发消息后保存到后端，回退到底部弹窗时自动刷新
 */
public class AgentActivity extends AppCompatActivity {

    private EditText etInput;
    private MaterialButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private OfflineAgentExecutor offlineExecutor;
    private String lastUserText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent);

        chatContainer = findViewById(R.id.chat_container);
        scrollView = findViewById(R.id.scroll_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        offlineExecutor = new OfflineAgentExecutor(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendCommand());

        loadChatHistory();
    }

    // ===================== 历史加载 =====================

    private void loadChatHistory() {
        if (!NetworkUtil.isConnected(this)) {
            addAgentBubble("🤖 你好！离线模式可用基础记账功能。");
            return;
        }
        addAgentBubble("⏳ 加载聊天记录...");
        ApiClient.getService().getAgentMessages(1, 50).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AgentChatMessage>>> call,
                                   Response<ApiResponse<List<AgentChatMessage>>> response) {
                chatContainer.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AgentChatMessage> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        for (AgentChatMessage msg : list) {
                            if ("user".equals(msg.role)) {
                                addUserBubble(msg.content);
                            } else {
                                addAgentBubble(msg.content);
                            }
                        }
                    } else {
                        addAgentBubble("🤖 你好！我是你的AI服装店助理。");
                    }
                    checkAndShowSuggestions();
                } else {
                    addAgentBubble("🤖 你好！我是你的AI服装店助理。");
                }
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<AgentChatMessage>>> call, Throwable t) {
                chatContainer.removeAllViews();
                addAgentBubble("🤖 你好！");
                scrollToBottom();
            }
        });
    }

    private void checkAndShowSuggestions() {
        List<AgentSuggestionManager.Suggestion> suggestions = AgentSuggestionManager.getSuggestions();
        if (!suggestions.isEmpty()) {
            StringBuilder sb = new StringBuilder("💡 温馨提示\n");
            for (int i = 0; i < Math.min(suggestions.size(), 2); i++) {
                sb.append("· ").append(suggestions.get(i).text).append("\n");
            }
            addAgentBubble(sb.toString().trim());
        }
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    // ===================== 消息发送 =====================

    private void sendCommand() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addUserBubble(text);
        lastUserText = text;
        etInput.setText("");
        btnSend.setEnabled(false);

        boolean online = NetworkUtil.isConnected(this);

        // Step 1: 本地规则引擎
        AgentRuleEngine.ParseResult localResult = AgentRuleEngine.parse(text);

        if (localResult.confidence >= 0.85) {
            btnSend.setEnabled(true);
            // 查询类 → 直接调 API 取数据
            if ("query_sales".equals(localResult.action)) {
                addAgentBubble("⏳ 正在查询经营数据...");
                querySalesSummary();
                return;
            }
            if ("stock_query".equals(localResult.action)) {
                addAgentBubble("⏳ 正在查询库存数据...");
                queryStock();
                return;
            }
            // 其他指令 → 弹确认
            if (online) {
                showConfirmDialog(localResult);
            } else {
                ExecuteResult execResult = offlineExecutor.execute(localResult);
                addAgentBubbleAndSave(execResult.message);
            }
            return;
        }

        if (!online) {
            btnSend.setEnabled(true);
            addAgentBubble("离线模式无法识别这条指令，联网后重试。");
            return;
        }

        // Step 2: DeepSeek 云端智能问答
        addAgentBubble("⏳ 正在分析您的问题...");
        HashMap<String, String> body = new HashMap<>();
        body.put("text", text);
        ApiClient.getService().chatWithAgent(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AgentChatResponse>> call,
                                   Response<ApiResponse<AgentChatResponse>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AgentChatResponse r = response.body().getData();
                    if (r != null && r.reply != null && !r.reply.isEmpty()) {
                        addAgentBubbleAndSave(r.reply);
                    } else {
                        addAgentBubbleAndSave("抱歉，暂时无法回答这个问题。");
                    }
                } else {
                    int code = response.code();
                    String msg = response.body() != null ? response.body().getMessage() : null;
                    if (code == 404) addAgentBubbleAndSave("🔌 后端智能问答接口未部署，请重启后端服务。");
                    else if (code == 500) addAgentBubbleAndSave("🔥 后端服务错误，请检查 DeepSeek API Key。");
                    else if (msg != null) addAgentBubbleAndSave("🤔 " + msg);
                    else addAgentBubbleAndSave("（HTTP " + code + "）");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AgentChatResponse>> call, Throwable t) {
                btnSend.setEnabled(true);
                if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    addAgentBubbleAndSave("⏰ AI 思考超时了，可以拆开问。");
                } else {
                    addAgentBubbleAndSave("📡 网络连接失败，请检查网络后重试。");
                }
            }
        });
    }

    // ===================== API 查询 =====================

    private void querySalesSummary() {
        ApiClient.getService().getAgentSummary().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AgentSummaryResponse>> call,
                                   Response<ApiResponse<AgentSummaryResponse>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AgentSummaryResponse data = response.body().getData();
                    if (data != null) {
                        addAgentCardBubble(data, buildRichSummary(data));
                    } else {
                        addAgentBubbleAndSave("暂时没有经营数据");
                    }
                } else {
                    addAgentBubbleAndSave("📊 查询失败，请稍后重试");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AgentSummaryResponse>> call, Throwable t) {
                btnSend.setEnabled(true);
                addAgentBubbleAndSave("📡 网络错误，无法查询经营数据");
            }
        });
    }

    private void queryStock() {
        ApiClient.getService().getLowStockProducts().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LowStockProduct>>> call,
                                   Response<ApiResponse<List<LowStockProduct>>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LowStockProduct> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("📋 库存不足商品 (").append(list.size()).append("件)\n─────────────────\n");
                        for (LowStockProduct p : list) {
                            sb.append(p.stock <= 2 ? "🔴" : "🟡").append(" ").append(p.productName);
                            if (p.attrsDisplay != null) sb.append("(").append(p.attrsDisplay).append(")");
                            sb.append(" 仅剩 ").append(p.stock).append(" 件\n");
                        }
                        addAgentBubbleAndSave(sb.toString().trim());
                    } else {
                        addAgentBubbleAndSave("✅ 库存充足，暂无库存不足的商品");
                    }
                } else {
                    addAgentBubbleAndSave("📋 查询失败，请稍后重试");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LowStockProduct>>> call, Throwable t) {
                btnSend.setEnabled(true);
                addAgentBubbleAndSave("📡 网络错误，无法查询库存");
            }
        });
    }

    // ===================== 确认执行 =====================

    private void showConfirmDialog(AgentRuleEngine.ParseResult result) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("执行确认")
                .setMessage(result.displayText)
                .setPositiveButton("确认执行", (dialog, which) -> executeAndSave(result))
                .setNegativeButton("取消", null)
                .show();
    }

    private void executeAndSave(AgentRuleEngine.ParseResult result) {
        AgentSuggestionManager.recordHomeOpen();

        switch (result.action) {
            case "open_page": {
                String page = result.params.get("arg1").toString();
                navigateToPage(page);
                addAgentBubbleAndSave("✅ 正在打开「" + page + "」");
                break;
            }
            case "system_setting":
                addAgentBubbleAndSave("✅ 设置已更新");
                break;
            default:
                addAgentBubbleAndSave("✅ 指令已记录");
                break;
        }
    }

    private void navigateToPage(String page) {
        Class<?> target = null;
        if (page.contains("经营") || page.contains("分析")) {
            try { target = Class.forName("com.heben.client.activity.AnalyticsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("商品")) target = ProductsActivity.class;
        else if (page.contains("分类")) target = CategoriesActivity.class;
        else if (page.contains("进货") || page.contains("入库")) target = PurchaseActivity.class;
        else if (page.contains("销售") || page.contains("开单")) target = SaleActivity.class;
        else if (page.contains("预约")) target = AppointmentsActivity.class;
        else if (page.contains("属性")) target = AttributesActivity.class;
        else if (page.contains("设置")) {
            try { target = Class.forName("com.heben.client.activity.ProgramSettingsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("共享") || page.contains("批发")) target = SharedProductsActivity.class;
        if (target != null) startActivity(new Intent(this, target));
    }

    private String buildRichSummary(AgentSummaryResponse data) {
        StringBuilder sb = new StringBuilder();
        if (data.todayBrief != null) {
            var brief = data.todayBrief;
            sb.append("📊 今日经营简报\n─────────────────\n");
            sb.append("💰 销售额\t¥").append(String.format("%.2f", brief.totalAmount)).append("\n");
            sb.append("📈 毛利\t\t¥").append(String.format("%.2f", brief.totalProfit)).append("\n");
            sb.append("📋 订单数\t").append(brief.orderCount).append(" 单\n");
            if (brief.summary != null) sb.append("📝 ").append(brief.summary).append("\n");
            sb.append("\n");
        }
        if (data.stockWarnings != null && !data.stockWarnings.isEmpty()) {
            sb.append("⚠️ 库存预警\n─────────────────\n");
            for (var w : data.stockWarnings) {
                sb.append(w.stock <= 2 ? "🔴" : "🟡").append(" ").append(w.productName);
                if (w.attrs != null) sb.append("(").append(w.attrs).append(")");
                sb.append(" 仅剩 ").append(w.stock).append(" 件\n");
            }
            sb.append("\n");
        }
        if (data.hotProducts != null && !data.hotProducts.isEmpty()) {
            sb.append("⭐ 热销商品\n─────────────────\n");
            for (int i = 0; i < data.hotProducts.size(); i++) {
                var h = data.hotProducts.get(i);
                sb.append(i + 1).append(". ").append(h.productName)
                        .append("  ×").append(h.salesCount)
                        .append("  ¥").append(String.format("%.1f", h.salesAmount)).append("\n");
            }
        }
        if (data.suggestion != null) sb.append("\n💡 ").append(data.suggestion);
        return sb.toString().trim();
    }

    // ===================== UI =====================

    private void addUserBubble(String text) {
        View bubble = LayoutInflater.from(this).inflate(R.layout.item_agent_bubble, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tv_bubble_text_user);
        tv.setText(text);
        bubble.findViewById(R.id.layout_agent).setVisibility(View.GONE);
        bubble.findViewById(R.id.layout_user).setVisibility(View.VISIBLE);
        chatContainer.addView(bubble);
        scrollToBottom();
    }

    private void addAgentBubble(String text) {
        View bubble = LayoutInflater.from(this).inflate(R.layout.item_agent_bubble, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tv_bubble_text);
        tv.setText(text);
        bubble.findViewById(R.id.layout_agent).setVisibility(View.VISIBLE);
        bubble.findViewById(R.id.layout_user).setVisibility(View.GONE);
        chatContainer.addView(bubble);
        scrollToBottom();
    }

    private void addAgentCardBubble(AgentSummaryResponse data, String fallbackText) {
        View card = AgentRichCardHelper.buildSummaryCard(this, data,
                () -> {
                    Intent i = new Intent(this, PurchaseActivity.class);
                    startActivity(i);
                },
                () -> {
                    Intent i = new Intent(this, AnalyticsActivity.class);
                    startActivity(i);
                });

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.setPadding(0, 0, 0, 12);

        TextView avatar = new TextView(this);
        avatar.setText("🤖");
        avatar.setTextSize(18);
        avatar.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp2px(36), dp2px(36));
        avatarLp.setMargins(0, dp2px(4), dp2px(8), 0);
        avatar.setLayoutParams(avatarLp);

        wrapper.addView(avatar);
        wrapper.addView(card);
        chatContainer.addView(wrapper);
        scrollToBottom();
        saveCardTextToBackend(fallbackText);
    }

    private void addAgentBubbleAndSave(String text) {
        addAgentBubble(text);
        saveCardTextToBackend(text);
    }

    private void saveCardTextToBackend(String msg) {
        if (lastUserText != null && msg != null) {
            HashMap<String, String> body = new HashMap<>();
            body.put("userMessage", lastUserText);
            body.put("agentMessage", msg);
            ApiClient.getService().saveAgentMessagePair(body).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
            });
        }
    }

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
