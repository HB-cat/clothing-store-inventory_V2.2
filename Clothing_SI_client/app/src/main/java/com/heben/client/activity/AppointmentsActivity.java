package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Appointment;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentsActivity extends AppCompatActivity {

    private LinearLayout layoutPending, layoutConfirmed, layoutHistory;
    private List<Appointment> allList = new ArrayList<>();
    private TextView tabPending, tabConfirmed, tabHistory;
    private View activeTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        layoutPending = findViewById(R.id.layout_pending);
        layoutConfirmed = findViewById(R.id.layout_confirmed);
        layoutHistory = findViewById(R.id.layout_history);

        tabPending = findViewById(R.id.tab_pending);
        tabConfirmed = findViewById(R.id.tab_confirmed);
        tabHistory = findViewById(R.id.tab_history);

        tabPending.setOnClickListener(v -> switchTab(tabPending, layoutPending));
        tabConfirmed.setOnClickListener(v -> switchTab(tabConfirmed, layoutConfirmed));
        tabHistory.setOnClickListener(v -> switchTab(tabHistory, layoutHistory));

        // 默认选中待确认
        activeTab = tabPending;
        tabPending.setSelected(true);

        loadAppointments();
    }

    private void switchTab(View tab, LinearLayout layout) {
        if (activeTab == tab) return;
        if (activeTab != null) activeTab.setSelected(false);
        tab.setSelected(true);
        activeTab = tab;

        layoutPending.setVisibility(tab == tabPending ? View.VISIBLE : View.GONE);
        layoutConfirmed.setVisibility(tab == tabConfirmed ? View.VISIBLE : View.GONE);
        layoutHistory.setVisibility(tab == tabHistory ? View.VISIBLE : View.GONE);

        if (layout.getChildCount() == 0) {
            renderAppointments(tab == tabPending ? "pending" : tab == tabConfirmed ? "confirmed" : "history", layout);
        }
    }

    private void loadAppointments() {
        ApiClient.getService().getAppointments("pending").enqueue(new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Appointment> data = response.body().getData();
                    if (data != null) allList = data;
                    renderAppointments("pending", layoutPending);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {}
        });
    }

    private void renderAppointments(String status, LinearLayout container) {
        container.removeAllViews();
        boolean hasItems = false;
        for (Appointment a : allList) {
            String s = a.status != null ? a.status : "pending";
            if (!s.equals(status)) continue;
            hasItems = true;
            View card = LayoutInflater.from(this).inflate(R.layout.item_appointment, container, false);
            ((TextView) card.findViewById(R.id.tv_appt_name)).setText(a.buyerName != null ? a.buyerName : "客户");
            ((TextView) card.findViewById(R.id.tv_appt_info)).setText(a.productName != null ? a.productName : "");
            ((TextView) card.findViewById(R.id.tv_appt_time)).setText(a.appointmentDate != null ? a.appointmentDate + " " + (a.timeSlot != null ? a.timeSlot : "") : "");
            container.addView(card);
        }
        if (!hasItems) {
            TextView empty = new TextView(this);
            empty.setText("暂无" + (status.equals("pending") ? "待确认" : status.equals("confirmed") ? "已确认" : "历史") + "预约");
            empty.setTextSize(13f);
            empty.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, 32, 0, 32);
            container.addView(empty);
        }
    }
}
