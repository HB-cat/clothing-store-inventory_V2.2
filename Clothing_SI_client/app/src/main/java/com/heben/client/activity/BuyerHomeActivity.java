package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Appointment;
import com.heben.client.model.Models.User;
import com.heben.client.network.ApiClient;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerHomeActivity extends AppCompatActivity {

    private LinearLayout layoutFollowedStores;
    private LinearLayout layoutAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_home);

        layoutFollowedStores = findViewById(R.id.layout_followed_stores);
        layoutAppointments = findViewById(R.id.layout_appointments);

        // 搜索输入框 → 跳转关注页搜索
        findViewById(R.id.et_search).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerFollowsActivity.class)));
        findViewById(R.id.et_search).setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
                startActivity(new Intent(this, BuyerFollowsActivity.class));
            }
        });

        findViewById(R.id.btn_buy).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerProductsActivity.class)));

        // 底部导航
        findViewById(R.id.bottom_messages).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerMessagesActivity.class)));
        findViewById(R.id.bottom_profile).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFollowedStores();
        loadAppointments();
    }

    private void loadFollowedStores() {
        layoutFollowedStores.removeAllViews();
        ApiClient.getService().getFollowedSellers().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    List<User> users = resp.body().getData();
                    if (users != null && !users.isEmpty()) {
                        for (User u : users) {
                            addStoreCard(u);
                        }
                    } else {
                        addHint(layoutFollowedStores, "还没有关注商家\n去搜索并关注您喜欢的店铺吧");
                    }
                } else {
                    addHint(layoutFollowedStores, "暂无关注商家");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                addHint(layoutFollowedStores, "网络错误");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addStoreCard(User u) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_shop_search, layoutFollowedStores, false);
        String shopName = u.shopName != null ? u.shopName : u.nickname;
        ((TextView) card.findViewById(R.id.tv_shop_name)).setText(shopName);
        ((TextView) card.findViewById(R.id.tv_shop_desc)).setText(
                (u.nickname != null ? u.nickname + " · " : "") +
                        (u.role == 2 ? "批发商" : "零售商"));

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuyerProductsActivity.class);
            intent.putExtra("sellerId", u.id);
            intent.putExtra("sellerName", shopName);
            startActivity(intent);
        });

        MaterialButton btn = card.findViewById(R.id.btn_follow);
        btn.setText("逛逛");
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuyerProductsActivity.class);
            intent.putExtra("sellerId", u.id);
            intent.putExtra("sellerName", shopName);
            startActivity(intent);
        });

        layoutFollowedStores.addView(card);
    }

    private void loadAppointments() {
        layoutAppointments.removeAllViews();
        ApiClient.getService().getAppointments(null).enqueue(new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    List<Appointment> data = resp.body().getData();
                    if (data != null && !data.isEmpty()) {
                        for (Appointment a : data) {
                            addAppointmentCard(a);
                        }
                    } else {
                        addHint(layoutAppointments, "暂无预约");
                    }
                } else {
                    addHint(layoutAppointments, "暂无预约");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {
                addHint(layoutAppointments, "暂无预约");
            }
        });
    }

    private void addAppointmentCard(Appointment a) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_appointment_card, layoutAppointments, false);
        if (card == null) {
            // fallback if layout not found
            TextView tv = new TextView(this);
            tv.setText(a.sellerName + " · " + a.appointmentDate + " " + a.timeSlot);
            tv.setTextSize(13);
            tv.setPadding(16, 12, 16, 12);
            tv.setBackgroundResource(android.R.color.white);
            layoutAppointments.addView(tv);
            return;
        }
        ((TextView) card.findViewById(R.id.tv_appt_seller)).setText(a.sellerName);
        ((TextView) card.findViewById(R.id.tv_appt_time)).setText(a.appointmentDate + " " + a.timeSlot);
        TextView tvStatus = card.findViewById(R.id.tv_appt_status);
        tvStatus.setText(getStatusText(a.status));
        tvStatus.setTextColor(getStatusColor(a.status));
        layoutAppointments.addView(card);
    }

    private String getStatusText(String status) {
        if (status == null) return "待确认";
        switch (status) {
            case "confirmed": return "已确认";
            case "arrived": return "已到店";
            case "completed": return "已完成";
            case "cancelled": return "已取消";
            default: return "待确认";
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFFE67E22;
        switch (status) {
            case "confirmed": return 0xFF27AE60; // 已确认 → 绿
            case "arrived": return 0xFF2980B9;   // 已到店 → 蓝
            case "completed": return 0xFF95A5A6; // 已完成 → 灰
            case "cancelled": return 0xFFE74C3C; // 已取消 → 红
            default: return 0xFFE67E22;          // 待确认 → 橙
        }
    }

    private void addHint(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setLineSpacing(4, 1);
        tv.setTextColor(0xFF95A5A6);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 20, 0, 20);
        parent.addView(tv);
    }
}
