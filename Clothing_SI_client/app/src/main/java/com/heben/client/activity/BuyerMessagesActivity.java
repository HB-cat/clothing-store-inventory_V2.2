package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.adapter.ChatSessionAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Appointment;
import com.heben.client.model.Models.ChatSession;
import com.heben.client.network.ApiClient;
import com.heben.client.util.BottomNav;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerMessagesActivity extends AppCompatActivity {

    private LinearLayout layoutAppointments;
    private RecyclerView rv;
    private ChatSessionAdapter adapter;
    private List<ChatSession> list = new ArrayList<>();
    private TextView tabChat, tabAppointment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_messages);

        BottomNav.setup(this, R.id.bottom_messages);

        layoutAppointments = findViewById(R.id.layout_appointments);
        rv = findViewById(R.id.rv_buyer_sessions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatSessionAdapter(list, session -> {
            Intent intent = new Intent(this, ChatDetailActivity.class);
            intent.putExtra("sessionId", session.id);
            intent.putExtra("otherName", session.otherName);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        tabChat = findViewById(R.id.tab_chat);
        tabAppointment = findViewById(R.id.tab_appointment);
        tabChat.setOnClickListener(v -> switchTab(true));
        tabAppointment.setOnClickListener(v -> switchTab(false));

        // 支持从外部 intent 直接切到预约 tab
        if (getIntent().getBooleanExtra("switch_to_appointment", false)) {
            switchTab(false);
        }
    }

    private void switchTab(boolean isChat) {
        if (isChat) {
            tabChat.setBackgroundResource(R.drawable.tab_active_bg);
            tabChat.setTextColor(ContextCompat.getColor(this, R.color.md_on_primary));
            tabChat.setTypeface(null, android.graphics.Typeface.BOLD);
            tabAppointment.setBackgroundResource(0);
            tabAppointment.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
            tabAppointment.setTypeface(null, android.graphics.Typeface.NORMAL);
            rv.setVisibility(View.VISIBLE);
            layoutAppointments.setVisibility(View.GONE);
        } else {
            tabAppointment.setBackgroundResource(R.drawable.tab_active_bg);
            tabAppointment.setTextColor(ContextCompat.getColor(this, R.color.md_on_primary));
            tabAppointment.setTypeface(null, android.graphics.Typeface.BOLD);
            tabChat.setBackgroundResource(0);
            tabChat.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
            tabChat.setTypeface(null, android.graphics.Typeface.NORMAL);
            rv.setVisibility(View.GONE);
            layoutAppointments.setVisibility(View.VISIBLE);
            loadAppointments();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatSessions();
    }

    private void loadAppointments() {
        layoutAppointments.removeAllViews();
        ApiClient.getService().getAppointments(null).enqueue(new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Appointment> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        for (Appointment a : data) addApptCard(a);
                    } else {
                        addHint("暂无预约");
                    }
                } else {
                    addHint("暂无预约");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {
                addHint("暂无预约");
            }
        });
    }

    private void addApptCard(Appointment a) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_appointment_card, layoutAppointments, false);
        ((TextView) card.findViewById(R.id.tv_appt_seller)).setText(a.sellerName);
        ((TextView) card.findViewById(R.id.tv_appt_time)).setText(
                (a.appointmentDate != null ? a.appointmentDate : "") + " " + (a.timeSlot != null ? a.timeSlot : ""));
        TextView tvStatus = card.findViewById(R.id.tv_appt_status);
        String st = a.status != null ? a.status : "";
        tvStatus.setText(getStatusText(st));
        tvStatus.setTextColor(getStatusColor(st));
        layoutAppointments.addView(card);
    }

    private String getStatusText(String s) {
        switch (s) {
            case "confirmed": return "已确认"; case "arrived": return "已到店";
            case "completed": return "已完成"; case "cancelled": return "已取消";
            default: return "待确认";
        }
    }

    private int getStatusColor(String s) {
        switch (s) {
            case "confirmed": return ContextCompat.getColor(this, R.color.md_success);
            case "arrived": return ContextCompat.getColor(this, R.color.md_secondary);
            case "completed": return ContextCompat.getColor(this, R.color.md_on_surface_variant);
            case "cancelled": return ContextCompat.getColor(this, R.color.md_error);
            default: return ContextCompat.getColor(this, R.color.md_tertiary_container);
        }
    }

    private void addHint(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 16, 0, 16);
        layoutAppointments.addView(tv);
    }

    private void loadChatSessions() {
        ApiClient.getService().getChatSessions().enqueue(new Callback<ApiResponse<List<ChatSession>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatSession>>> call, Response<ApiResponse<List<ChatSession>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<ChatSession> data = response.body().getData();
                    if (data != null) {
                        list.clear();
                        list.addAll(data);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<ChatSession>>> call, Throwable t) {
            }
        });
    }
}
