package com.heben.client.widget;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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
 * 智能体底部弹窗 — 与 AgentPanelActivity 共享同一份聊天历史
 */
public class AgentBottomSheetDialog extends BottomSheetDialogFragment {

    private EditText etInput;
    private MaterialButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private OfflineAgentExecutor offlineExecutor;
    private boolean historyLoaded = false;
    private String lastUserText = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_agent_bottom_sheet, container, false);

        chatContainer = v.findViewById(R.id.chat_sheet_container);
        scrollView = v.findViewById(R.id.scroll_sheet_chat);
        etInput = v.findViewById(R.id.et_sheet_input);
        btnSend = v.findViewById(R.id.btn_sheet_send);
        offlineExecutor = new OfflineAgentExecutor(requireContext());

        v.findViewById(R.id.btn_sheet_close).setOnClickListener(x -> dismiss());
        v.findViewById(R.id.btn_sheet_fullscreen).setOnClickListener(x -> {
            dismiss();
            Intent fullIntent = new Intent(requireContext(), com.heben.client.activity.AgentActivity.class);
            fullIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(fullIntent);
        });
        btnSend.setOnClickListener(x -> sendCommand());

        loadChatHistory();
        return v;
    }

    // ===================== 历史加载 =====================

    private void loadChatHistory() {
        if (!NetworkUtil.isConnected(requireContext())) {
            if (!historyLoaded) {
                addAgentBubble("🤖 你好！离线模式可用基础记账功能。");
                historyLoaded = true;
            }
            return;
        }
        ApiClient.getService().getAgentMessages(1, 50).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AgentChatMessage>>> call,
                                   Response<ApiResponse<List<AgentChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AgentChatMessage> list = response.body().getData();
                    chatContainer.removeAllViews();
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
                    historyLoaded = true;
                    checkAndShowSuggestions();
                    scrollToBottom();
                } else {
                    if (!historyLoaded) {
                        addAgentBubble("🤖 你好！我是你的AI服装店助理。");
                        historyLoaded = true;
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<AgentChatMessage>>> call, Throwable t) {
                if (!historyLoaded) {
                    addAgentBubble("🤖 你好！");
                    historyLoaded = true;
                }
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

        boolean online = NetworkUtil.isConnected(requireContext());

        // Step 1: 本地规则引擎
        AgentRuleEngine.ParseResult localResult = AgentRuleEngine.parse(text);

        if (localResult.confidence >= 0.85) {
            btnSend.setEnabled(true);
            if (online) {
                showConfirmDialog(localResult);
            } else {
                ExecuteResult execResult = offlineExecutor.execute(localResult);
                addAgentBubble(execResult.message);
            }
            return;
        }

        if (!online) {
            btnSend.setEnabled(true);
            addAgentBubble("离线模式无法识别这条指令，联网后重试。");
            return;
        }

        // Step 2: DeepSeek 云端智能问答（后端自动保存）
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
                        addAgentBubble(r.reply);
                    } else {
                        addAgentBubble("抱歉，暂时无法回答这个问题。");
                    }
                } else {
                    int httpCode = response.code();
                    String serverMsg = response.body() != null ? response.body().getMessage() : null;
                    if (httpCode == 404) {
                        addAgentBubble("🔌 后端智能问答接口未部署，请重启后端服务。");
                    } else if (httpCode == 500) {
                        addAgentBubble("🔥 后端服务错误，请检查 DeepSeek API Key。");
                    } else if (serverMsg != null) {
                        addAgentBubble("🤔 " + serverMsg);
                    } else {
                        addAgentBubble("（HTTP " + httpCode + "）");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AgentChatResponse>> call, Throwable t) {
                btnSend.setEnabled(true);
                if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    addAgentBubble("⏰ AI 思考超时了，可以拆开问。");
                } else {
                    addAgentBubble("📡 网络连接失败，请检查网络后重试。");
                }
            }
        });
    }

    /** 添加智能体气泡 + 保存到后端 */
    private void addAgentBubbleAndSave(String text) {
        addAgentBubble(text);
        if (lastUserText != null) {
            HashMap<String, String> body = new HashMap<>();
            body.put("userMessage", lastUserText);
            body.put("agentMessage", text);
            ApiClient.getService().saveAgentMessagePair(body).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
            });
        }
    }

    // ===================== 确认+执行 =====================

    private void showConfirmDialog(AgentRuleEngine.ParseResult result) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("执行确认")
                .setMessage(result.displayText)
                .setPositiveButton("确认执行", (dialog, which) -> {
                    if (isSensitiveAction(result.action)) {
                        showPasswordVerifyDialog(result);
                    } else {
                        executeAndFeedback(result);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showPasswordVerifyDialog(AgentRuleEngine.ParseResult result) {
        final EditText input = new EditText(requireContext());
        input.setHint("请输入登录密码确认");
        input.setPadding(40, 20, 40, 20);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("安全验证")
                .setMessage("此操作需要密码确认")
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {
                    String pwd = input.getText().toString().trim();
                    if (pwd.isEmpty()) {
                        Toast.makeText(requireContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    HashMap<String, String> body = new HashMap<>();
                    body.put("password", pwd);
                    ApiClient.getService().verifyPassword(body).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                                executeAndFeedback(result);
                            } else {
                                Toast.makeText(requireContext(), "密码错误", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean isSensitiveAction(String action) {
        return false;
    }

    /** 执行并反馈 — 用 addAgentBubbleAndSave 持久化 */
    private void executeAndFeedback(AgentRuleEngine.ParseResult result) {
        AgentSuggestionManager.recordHomeOpen();

        switch (result.action) {
            case "open_page": {
                String page = result.params.get("arg1").toString();
                navigateToPage(page);
                addAgentBubbleAndSave("✅ 正在打开「" + page + "」");
                break;
            }
            case "system_setting": {
                addAgentBubbleAndSave("✅ 设置已更新");
                break;
            }
            case "stock_query": {
                addAgentBubble("⏳ 正在查询库存数据...");
                ApiClient.getService().getLowStockProducts().enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<LowStockProduct>>> call,
                                           Response<ApiResponse<List<LowStockProduct>>> response) {
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
                        addAgentBubbleAndSave("📡 网络错误，无法查询库存");
                    }
                });
                break;
            }
            case "query_sales": {
                addAgentBubble("⏳ 正在查询经营数据...");
                ApiClient.getService().getAgentSummary().enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AgentSummaryResponse>> call,
                                           Response<ApiResponse<AgentSummaryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            AgentSummaryResponse data = response.body().getData();
                            if (data != null) {
                                // 富文本卡片展示 + 文本兜底
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
                        addAgentBubbleAndSave("📡 网络错误，无法查询经营数据");
                    }
                });
                break;
            }
            default:
                addAgentBubbleAndSave("✅ 指令已记录");
                break;
        }

        if (result.params.containsKey("arg1") && result.action.equals("sale")) {
            AgentSuggestionManager.recordProductUsed(result.params.get("arg1").toString());
        }
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
            sb.append("💡 说「进货」可快速补货\n\n");
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

    private void navigateToPage(String page) {
        Class<?> target = null;
        if (page.contains("经营") || page.contains("分析")) {
            try { target = Class.forName("com.heben.client.activity.AnalyticsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("商品")) {
            target = com.heben.client.activity.ProductsActivity.class;
        } else if (page.contains("分类")) {
            target = com.heben.client.activity.CategoriesActivity.class;
        } else if (page.contains("进货") || page.contains("入库")) {
            target = com.heben.client.activity.PurchaseActivity.class;
        } else if (page.contains("销售") || page.contains("开单")) {
            target = com.heben.client.activity.SaleActivity.class;
        } else if (page.contains("预约")) {
            target = com.heben.client.activity.AppointmentsActivity.class;
        } else if (page.contains("属性")) {
            target = com.heben.client.activity.AttributesActivity.class;
        } else if (page.contains("设置")) {
            try { target = Class.forName("com.heben.client.activity.ProgramSettingsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("共享") || page.contains("批发")) {
            target = com.heben.client.activity.SharedProductsActivity.class;
        }
        if (target != null) startActivity(new Intent(requireContext(), target));
    }

    // ===================== UI =====================

    private void addUserBubble(String text) {
        View bubble = LayoutInflater.from(requireContext()).inflate(R.layout.item_agent_bubble, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tv_bubble_text_user);
        tv.setText(text);
        bubble.findViewById(R.id.layout_agent).setVisibility(View.GONE);
        bubble.findViewById(R.id.layout_user).setVisibility(View.VISIBLE);
        chatContainer.addView(bubble);
        scrollToBottom();
    }

    private void addAgentBubble(String text) {
        View bubble = LayoutInflater.from(requireContext()).inflate(R.layout.item_agent_bubble, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tv_bubble_text);
        tv.setText(text);
        bubble.findViewById(R.id.layout_agent).setVisibility(View.VISIBLE);
        bubble.findViewById(R.id.layout_user).setVisibility(View.GONE);
        chatContainer.addView(bubble);
        scrollToBottom();
    }

    /** 添加富文本经营简报卡片到聊天区 */
    private void addAgentCardBubble(AgentSummaryResponse data, String fallbackText) {
        // 构建卡片 View
        View card = AgentRichCardHelper.buildSummaryCard(requireContext(), data,
                // 去补货按钮 → 打开进货页面
                () -> {
                    Intent i = new Intent(requireContext(), com.heben.client.activity.PurchaseActivity.class);
                    startActivity(i);
                },
                // 详细分析按钮 → 打开经营分析页面
                () -> {
                    Intent i = new Intent(requireContext(), com.heben.client.activity.AnalyticsActivity.class);
                    startActivity(i);
                });

        // 包装：左侧头像 + 卡片
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.setPadding(0, 0, 0, 12);

        // 头像
        TextView avatar = new TextView(requireContext());
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

        // 同时保存文本版本到后端（持久化）
        saveCardTextToBackend(fallbackText);
    }

    /** 保存卡片文本版本到后端聊天历史 */
    private void saveCardTextToBackend(String fallbackText) {
        if (lastUserText != null && fallbackText != null) {
            HashMap<String, String> body = new HashMap<>();
            body.put("userMessage", lastUserText);
            body.put("agentMessage", fallbackText);
            ApiClient.getService().saveAgentMessagePair(body).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
            });
        }
    }

    private int dp2px(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}
