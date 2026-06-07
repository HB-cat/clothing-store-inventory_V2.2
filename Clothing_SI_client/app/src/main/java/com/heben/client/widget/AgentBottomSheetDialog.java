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
import com.heben.client.activity.AgentPanelActivity;
import com.heben.client.agent.AgentRuleEngine;
import com.heben.client.agent.AgentSuggestionManager;
import com.heben.client.agent.OfflineAgentExecutor;
import com.heben.client.agent.OfflineAgentExecutor.ExecuteResult;
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.AgentParseResponse;
import com.heben.client.network.ApiClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 智能体底部弹窗卡片
 * 点击展开按钮跳转全屏 AgentPanelActivity
 */
public class AgentBottomSheetDialog extends BottomSheetDialogFragment {

    private EditText etInput;
    private MaterialButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private OfflineAgentExecutor offlineExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_agent_bottom_sheet, container, false);

        chatContainer = v.findViewById(R.id.chat_sheet_container);
        scrollView = v.findViewById(R.id.scroll_sheet_chat);
        etInput = v.findViewById(R.id.et_sheet_input);
        btnSend = v.findViewById(R.id.btn_sheet_send);
        offlineExecutor = new OfflineAgentExecutor(requireContext());

        // 展开 → 跳转完整页面
        v.findViewById(R.id.btn_sheet_expand).setOnClickListener(x -> {
            Intent intent = new Intent(getActivity(), AgentPanelActivity.class);
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                intent.putExtra("agent_command", text);
            }
            startActivity(intent);
            dismiss();
        });

        // 关闭
        v.findViewById(R.id.btn_sheet_close).setOnClickListener(x -> dismiss());

        // 发送
        btnSend.setOnClickListener(x -> sendCommand());

        // 欢迎消息 + 建议
        addAgentBubble("你好！我是你的服装店智能助手。\n\n试试说出：\n「卖了2件白T恤」\n「新建入库单」\n「今天的销售情况」");

        // 主动建议：通知形式展示
        checkSuggestions();

        return v;
    }

    private void checkSuggestions() {
        java.util.List<AgentSuggestionManager.Suggestion> suggestions = AgentSuggestionManager.getSuggestions();
        if (!suggestions.isEmpty()) {
            StringBuilder sb = new StringBuilder("💡 温馨提示\n");
            for (int i = 0; i < Math.min(suggestions.size(), 2); i++) {
                sb.append("· ").append(suggestions.get(i).text).append("\n");
            }
            addAgentBubble(sb.toString().trim());
        }
    }

    private void sendCommand() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addUserBubble(text);
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

        // 本地未匹配
        if (!online) {
            btnSend.setEnabled(true);
            addAgentBubble("离线模式无法识别这条指令，联网后重试或换个说法试试。");
            return;
        }

        // Step 2: DeepSeek 云端 fallback
        HashMap<String, String> body = new HashMap<>();
        body.put("text", text);
        ApiClient.getService().parseAgentCommand(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AgentParseResponse>> call,
                                   Response<ApiResponse<AgentParseResponse>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AgentParseResponse r = response.body().getData();
                    if (r != null && r.displayText != null) {
                        AgentRuleEngine.ParseResult cloudResult = new AgentRuleEngine.ParseResult(
                                r.action != null ? r.action : "cloud",
                                0.9, r.displayText);
                        showConfirmDialog(cloudResult);
                    } else {
                        addAgentBubble("已收到指令");
                    }
                } else {
                    addAgentBubble("抱歉，我没有理解这条指令。试试换个说法？");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AgentParseResponse>> call, Throwable t) {
                btnSend.setEnabled(true);
                addAgentBubble("网络连接失败，请检查网络后重试。");
            }
        });
    }

    private void showConfirmDialog(AgentRuleEngine.ParseResult result) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("执行确认")
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
        return "delete_category".equals(action)
                || "submit_bill".equals(action)
                || "return".equals(action)
                || "reject_appointment".equals(action);
    }

    private void executeAndFeedback(AgentRuleEngine.ParseResult result) {
        AgentSuggestionManager.recordHomeOpen();

        switch (result.action) {
            case "open_page": {
                String page = result.params.get("arg1").toString();
                navigateToPage(page);
                addAgentBubble("✅ 正在打开「" + page + "」");
                break;
            }
            case "system_setting": {
                addAgentBubble("✅ 设置已更新");
                break;
            }
            case "stock_query": {
                Intent i = new Intent(requireContext(), com.heben.client.activity.ProductsActivity.class);
                i.putExtra("filter_low_stock", true);
                startActivity(i);
                addAgentBubble("✅ 已为您打开库存不足商品列表");
                break;
            }
            case "query_sales": {
                startActivity(new Intent(requireContext(), com.heben.client.activity.AnalyticsActivity.class));
                addAgentBubble("✅ 正在查询销售数据...");
                break;
            }
            default:
                addAgentBubble("✅ 指令已记录，请在相应页面查看结果");
                break;
        }

        if (result.params.containsKey("arg1") && result.action.equals("sale")) {
            AgentSuggestionManager.recordProductUsed(result.params.get("arg1").toString());
        }
    }

    private void navigateToPage(String page) {
        Class<?> target = null;
        if (page.contains("经营") || page.contains("分析") || page.contains("数据")) {
            try { target = Class.forName("com.heben.client.activity.AnalyticsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("商品") || page.contains("产品")) {
            target = com.heben.client.activity.ProductsActivity.class;
        } else if (page.contains("分类") || page.contains("类目")) {
            target = com.heben.client.activity.CategoriesActivity.class;
        } else if (page.contains("进货") || page.contains("入库") || page.contains("采购")) {
            target = com.heben.client.activity.PurchaseActivity.class;
        } else if (page.contains("订单") || page.contains("销售") || page.contains("开单")) {
            target = com.heben.client.activity.SaleActivity.class;
        } else if (page.contains("预约") || page.contains("日程")) {
            target = com.heben.client.activity.AppointmentsActivity.class;
        } else if (page.contains("属性") || page.contains("尺码") || page.contains("颜色")) {
            target = com.heben.client.activity.AttributesActivity.class;
        } else if (page.contains("我") || page.contains("个人") || page.contains("设置")) {
            try { target = Class.forName("com.heben.client.activity.ProgramSettingsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("共享") || page.contains("批发") || page.contains("协作")) {
            target = com.heben.client.activity.SharedProductsActivity.class;
        }
        if (target != null) startActivity(new Intent(requireContext(), target));
    }

    // ===== UI =====

    private void addUserBubble(String text) {
        View bubble = LayoutInflater.from(requireContext()).inflate(R.layout.item_agent_bubble, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tv_bubble_text);
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

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
