package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.adapter.ChatMessageAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.ChatMessage;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;
    private MaterialButton btnSend;

    private long sessionId;
    private Long otherUserId;
    private String otherName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sessionId = getIntent().getLongExtra("sessionId", 0);
        otherUserId = getIntent().hasExtra("otherUserId") ? getIntent().getLongExtra("otherUserId", 0) : null;
        otherName = getIntent().getStringExtra("otherName");

        // 标题栏显示对方名称
        if (otherName != null) {
            TextView tvTitle = findViewById(R.id.tv_chat_title);
            if (tvTitle != null) tvTitle.setText(otherName);
        }

        rv = findViewById(R.id.rv_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messages);
        rv.setAdapter(adapter);

        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> sendMessage());

        // 如果有otherUserId但没有sessionId，先创建会话
        if (sessionId <= 0 && otherUserId != null) {
            createSessionAndLoad();
        } else if (sessionId > 0) {
            loadMessages();
        } else {
            Toast.makeText(this, "无法开始聊天", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createSessionAndLoad() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("otherUserId", otherUserId);

        ApiClient.getService().createChatSession(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();
                    if (data != null && data.get("id") != null) {
                        sessionId = ((Number) data.get("id")).longValue();
                        loadMessages();
                    }
                } else {
                    Toast.makeText(ChatDetailActivity.this, "创建会话失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(ChatDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        if (sessionId <= 0) return;

        ApiClient.getService().getChatMessages(sessionId, 1, 50).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessage>>> call,
                                   Response<ApiResponse<List<ChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    messages.clear();
                    List<ChatMessage> data = response.body().getData();
                    if (data != null) messages.addAll(data);
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) rv.smoothScrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessage>>> call, Throwable t) {
            }
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sessionId <= 0) {
            Toast.makeText(this, "会话未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);

        HashMap<String, Object> body = new HashMap<>();
        body.put("messageType", 1);
        body.put("content", content);

        ApiClient.getService().sendMessage(sessionId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    etMessage.setText("");
                    loadMessages();
                } else {
                    Toast.makeText(ChatDetailActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                btnSend.setEnabled(true);
                Toast.makeText(ChatDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
