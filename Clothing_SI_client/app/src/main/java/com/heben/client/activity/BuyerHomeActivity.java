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
import com.heben.client.util.BottomNav;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerHomeActivity extends AppCompatActivity {

    private LinearLayout layoutFollowedStores, layoutAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_home);

        layoutFollowedStores = findViewById(R.id.layout_followed_stores);
        layoutAppointments = findViewById(R.id.layout_appointments);

        BottomNav.setup(this, R.id.bottom_home);

        findViewById(R.id.et_search).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerFollowsActivity.class)));

        findViewById(R.id.btn_buy).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerProductsActivity.class)));
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
                        for (User u : users) addStoreCard(u);
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

    private void addStoreCard(User u) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_shop_search, layoutFollowedStores, false);
        String shopName = u.shopName != null ? u.shopName : u.nickname;
        ((TextView) card.findViewById(R.id.tv_shop_name)).setText(shopName);
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
                        for (Appointment a : data) addAppointmentCard(a);
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
            case "confirmed": return 0xFF27AE60; case "arrived": return 0xFF2980B9;
            case "completed": return 0xFF95A5A6; case "cancelled": return 0xFFE74C3C;
            default: return 0xFFE67E22;
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
