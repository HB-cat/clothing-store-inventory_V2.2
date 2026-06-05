package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.adapter.ChatSessionAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.ChatSession;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerMessagesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatSessionAdapter adapter;
    private List<ChatSession> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_messages);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_buyer_sessions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatSessionAdapter(list, session -> {
            Intent intent = new Intent(this, ChatDetailActivity.class);
            intent.putExtra("sessionId", session.id);
            intent.putExtra("otherName", session.otherName);
            startActivity(intent);
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.getService().getChatSessions().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatSession>>> call, Response<ApiResponse<List<ChatSession>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    list.clear();
                    List<ChatSession> data = response.body().getData();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<ChatSession>>> call, Throwable t) {}
        });
    }
}
