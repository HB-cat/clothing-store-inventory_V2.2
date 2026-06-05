package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.adapter.ChatMessageAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.ChatMessage;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private TextInputEditText etMessage;
    private MaterialButton btnSend;
    private long sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sessionId = getIntent().getLongExtra("sessionId", 0);
        String otherName = getIntent().getStringExtra("otherName");
        if (otherName != null) {
            setTitle(otherName);
        }

        rv = findViewById(R.id.rv_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messages);
        rv.setAdapter(adapter);

        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        if (sessionId <= 0) return;
        ApiClient.getService().getChatMessages(sessionId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessage>>> call, Response<ApiResponse<List<ChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    messages.clear();
                    List<ChatMessage> data = response.body().getData();
                    if (data != null) messages.addAll(data);
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) rv.smoothScrollToPosition(messages.size() - 1);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessage>>> call, Throwable t) {}
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSend.setEnabled(false);

        HashMap<String, Object> body = new HashMap<>();
        body.put("sessionId", sessionId);
        body.put("content", content);
        ApiClient.getService().sendMessage(body).enqueue(new Callback<ApiResponse<Void>>() {
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
