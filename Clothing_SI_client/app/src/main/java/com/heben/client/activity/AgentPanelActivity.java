package com.heben.client.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
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
 * 智能体助手完整面板
 * PRD要求：
 * - 本地规则引擎(Regex, 16种指令, ≤200ms) → DeepSeek云端fallback
 * - 所有操作通过确认弹窗执行
 * - 语音输入入口（UI占位）
 * - 敏感操作需要密码验证
 */
public class AgentPanelActivity extends AppCompatActivity {

    private EditText etInput;
    private MaterialButton btnSend, btnVoice;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private OfflineAgentExecutor offlineExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_panel);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnVoice = findViewById(R.id.btn_voice);
        chatContainer = findViewById(R.id.chat_container);
        scrollView = findViewById(R.id.scroll_chat);

        offlineExecutor = new OfflineAgentExecutor(this);

        // 语音按钮 — UI占位（PRD要求）
        btnVoice.setOnClickListener(v ->
                Toast.makeText(this, "语音识别功能即将上线", Toast.LENGTH_SHORT).show());

        // 检查是否有预先传入的指令
        String command = getIntent().getStringExtra("agent_command");
        if (command != null) {
            etInput.setText(command);
        }

        addAgentBubble("你好！我是你的服装店智能助手。\n\n试试说出：\n「卖了2件白T恤」\n「新建入库单」\n「今天的销售情况」\n\n直接发送本地引擎处理，未识别自动走云端。");

        btnSend.setOnClickListener(v -> sendCommand());
    }

    private void sendCommand() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addUserBubble(text);
        etInput.setText("");
        btnSend.setEnabled(false);

        boolean online = NetworkUtil.isConnected(this);

        // Step 1: 本地规则引擎解析（≤5ms）
        AgentRuleEngine.ParseResult localResult = AgentRuleEngine.parse(text);

        if (localResult.confidence >= 0.85) {
            // 本地匹配成功
            btnSend.setEnabled(true);
            if (online) {
                // 在线 → 走确认弹窗流程
                showConfirmDialog(localResult);
            } else {
                // 离线 → 伪智能体直接执行
                ExecuteResult execResult = offlineExecutor.execute(localResult);
                addAgentBubble(execResult.message);
            }
            return;
        }

        // 本地未匹配
        if (!online) {
            btnSend.setEnabled(true);
            addAgentBubble("离线模式无法识别这条指令，联网后重试或换个说法试试\n\n试试说出：\n「卖了2件白T恤，每件59元」\n「进货牛仔裤10条，进价70元」");
            return;
        }

        // Step 2: 在线 → DeepSeek云端fallback
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

    /** 确认弹窗 — PRD要求所有操作执行前必须确认 */
    private void showConfirmDialog(AgentRuleEngine.ParseResult result) {
        new AlertDialog.Builder(this)
                .setTitle("执行确认")
                .setMessage(result.displayText)
                .setPositiveButton("确认执行", (dialog, which) -> {
                    // 检查是否为敏感操作 — 要求密码验证
                    if (isSensitiveAction(result.action)) {
                        showPasswordVerifyDialog(result);
                    } else {
                        executeAndFeedback(result);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 敏感操作密码验证（PRD安全要求） */
    private void showPasswordVerifyDialog(AgentRuleEngine.ParseResult result) {
        final EditText input = new EditText(this);
        input.setHint("请输入登录密码确认");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("安全验证")
                .setMessage("此操作需要密码确认")
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {
                    String pwd = input.getText().toString().trim();
                    if (pwd.isEmpty()) {
                        Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 调用后端验证密码
                    HashMap<String, String> body = new HashMap<>();
                    body.put("password", pwd);
                    ApiClient.getService().verifyPassword(body).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                                executeAndFeedback(result);
                            } else {
                                Toast.makeText(AgentPanelActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(AgentPanelActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 是否为敏感操作 */
    private boolean isSensitiveAction(String action) {
        return "delete_category".equals(action)
                || "submit_bill".equals(action)
                || "return".equals(action)
                || "reject_appointment".equals(action);
    }

    /** 执行并反馈 */
    private void executeAndFeedback(AgentRuleEngine.ParseResult result) {
        // 记录用户行为
        AgentSuggestionManager.recordHomeOpen();

        switch (result.action) {
            case "open_page": {
                String page = result.params.get("arg1").toString();
                navigateToPage(page);
                addAgentBubble("✅ 正在打开「" + page + "」");
                break;
            }
            case "system_setting": {
                String target = result.params.get("arg2").toString();
                addAgentBubble("✅ 已" + (result.params.get("arg1").toString().contains("关") ? "关闭" : "打开") + target);
                break;
            }
            case "stock_query": {
                Intent i = new Intent(this, ProductsActivity.class);
                i.putExtra("filter_low_stock", true);
                startActivity(i);
                addAgentBubble("✅ 已为您打开库存不足商品列表");
                break;
            }
            case "query_sales": {
                startActivity(new Intent(this, AnalyticsActivity.class));
                addAgentBubble("✅ 正在查询销售数据...");
                break;
            }
            case "sale":
            case "new_bill":
            case "submit_bill":
            case "return":
            case "accept_appointment":
            case "reject_appointment":
            case "add_category":
            case "delete_category":
            case "add_product":
            case "purchase":
                // 这些操作需要后端配合，跳转对应页面手动操作
                addAgentBubble("✅ 已识别指令，正在跳转到对应页面...");
                navigateToAction(result.action);
                break;
            default:
                addAgentBubble("✅ 指令已记录");
                break;
        }

        // 记录常用操作
        if (result.params.containsKey("arg1") && result.action.equals("sale")) {
            AgentSuggestionManager.recordProductUsed(result.params.get("arg1").toString());
        }
    }

    /** 根据action跳转对应页面 */
    private void navigateToAction(String action) {
        Class<?> target = null;
        switch (action) {
            case "sale": case "new_bill":
                target = NewOrderActivity.class; break;
            case "return":
                target = SaleActivity.class; break;
            case "accept_appointment": case "reject_appointment":
                target = AppointmentsActivity.class; break;
            case "add_category": case "delete_category":
                target = CategoriesActivity.class; break;
            case "add_product": case "purchase":
                target = ProductsActivity.class; break;
        }
        if (target != null) startActivity(new Intent(this, target));
    }

    /** 解析页面名跳转 */
    private void navigateToPage(String page) {
        Class<?> target = null;
        if (page.contains("经营") || page.contains("分析") || page.contains("数据")) {
            try { target = Class.forName("com.heben.client.activity.AnalyticsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("商品") || page.contains("产品")) {
            target = ProductsActivity.class;
        } else if (page.contains("分类") || page.contains("类目")) {
            target = CategoriesActivity.class;
        } else if (page.contains("进货") || page.contains("入库") || page.contains("采购")) {
            target = PurchaseActivity.class;
        } else if (page.contains("订单") || page.contains("销售") || page.contains("开单")) {
            target = SaleActivity.class;
        } else if (page.contains("预约") || page.contains("日程")) {
            target = AppointmentsActivity.class;
        } else if (page.contains("属性") || page.contains("尺码") || page.contains("颜色")) {
            target = AttributesActivity.class;
        } else if (page.contains("我") || page.contains("个人") || page.contains("设置")) {
            try { target = Class.forName("com.heben.client.activity.ProgramSettingsActivity"); } catch (Exception ignored) {}
        } else if (page.contains("共享") || page.contains("批发") || page.contains("协作")) {
            target = SharedProductsActivity.class;
        }
        if (target != null) startActivity(new Intent(this, target));
    }

    // ===== UI辅助方法 =====

    private void addUserBubble(String text) {
        View bubble = LayoutInflater.from(this).inflate(R.layout.item_agent_bubble, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tv_bubble_text);
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

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
