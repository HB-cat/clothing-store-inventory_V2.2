package com.heben.client.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.AgentParseResponse;
import com.heben.client.network.ApiClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgentPanelActivity extends AppCompatActivity {

    private EditText etInput;
    private MaterialButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_panel);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        chatContainer = findViewById(R.id.chat_container);
        scrollView = findViewById(R.id.scroll_chat);

        addAgentBubble("你好！我是你的服装店智能助手。\n\n试试说出：\n「卖了2件白T恤」\n「新建入库单」\n「今天的销售情况」");

        btnSend.setOnClickListener(v -> sendCommand());
    }

    private void sendCommand() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addUserBubble(text);
        etInput.setText("");
        btnSend.setEnabled(false);

        HashMap<String, String> body = new HashMap<>();
        body.put("text", text);
        ApiClient.getService().parseAgentCommand(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AgentParseResponse>> call, Response<ApiResponse<AgentParseResponse>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AgentParseResponse r = response.body().getData();
                    if (r != null && r.displayText != null) {
                        addAgentBubble(r.displayText);
                    } else {
                        addAgentBubble("已收到指令（动作: " + (r != null ? r.action : "未知") + "）");
                    }
                } else {
                    addAgentBubble("抱歉，我没有理解这条指令。");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AgentParseResponse>> call, Throwable t) {
                btnSend.setEnabled(true);
                addAgentBubble("网络错误，请稍后再试。");
            }
        });
    }

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
