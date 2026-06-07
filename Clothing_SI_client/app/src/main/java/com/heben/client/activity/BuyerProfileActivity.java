package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.util.TokenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerProfileActivity extends AppCompatActivity {

    private TextView tvName, tvPhone;
    private TextView tvAge, tvHeight, tvWeight, tvBust, tvWaist, tvHip;
    private LinearLayout layoutStyles, layoutOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_profile);

        tvName = findViewById(R.id.tv_buyer_name);
        tvPhone = findViewById(R.id.tv_buyer_phone);
        tvAge = findViewById(R.id.tv_age);
        tvHeight = findViewById(R.id.tv_height);
        tvWeight = findViewById(R.id.tv_weight);
        tvBust = findViewById(R.id.tv_bust);
        tvWaist = findViewById(R.id.tv_waist);
        tvHip = findViewById(R.id.tv_hip);
        layoutStyles = findViewById(R.id.layout_styles);
        layoutOrders = findViewById(R.id.layout_orders);

        String nickname = TokenManager.getNickname();
        String username = TokenManager.getUsername();
        tvName.setText(nickname != null ? nickname : "买家");
        tvPhone.setText(username != null ? username : "");

        // 编辑客户信息
        findViewById(R.id.btn_edit_info).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerInfoEditActivity.class)));

        // 退出登录
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            TokenManager.logout();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });

        // 底部导航
        findViewById(R.id.bottom_home).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerHomeActivity.class)));
        findViewById(R.id.bottom_messages).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerMessagesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBuyerInfo();
        loadOrders();
    }

    @SuppressWarnings("unchecked")
    private void loadBuyerInfo() {
        ApiClient.getService().getBuyerInfo().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> info = response.body().getData();
                    if (info != null && !info.isEmpty()) {
                        if (info.get("age") != null) tvAge.setText(String.valueOf(info.get("age")));
                        if (info.get("height") != null) tvHeight.setText(String.valueOf(info.get("height")) + "cm");
                        if (info.get("weight") != null) tvWeight.setText(String.valueOf(info.get("weight")) + "kg");
                        if (info.get("bust") != null) tvBust.setText(String.valueOf(info.get("bust")) + "cm");
                        if (info.get("waist") != null) tvWaist.setText(String.valueOf(info.get("waist")) + "cm");
                        if (info.get("hip") != null) tvHip.setText(String.valueOf(info.get("hip")) + "cm");

                        // 穿衣风格
                        layoutStyles.removeAllViews();
                        Object styleObj = info.get("stylePreference");
                        if (styleObj instanceof String) {
                            String style = (String) styleObj;
                            if (!style.isEmpty()) {
                                for (String s : style.split(",")) {
                                    TextView tag = new TextView(BuyerProfileActivity.this);
                                    tag.setText(s.trim());
                                    tag.setTextSize(11);
                                    tag.setTextColor(0xFFE85D3A);
                                    tag.setBackgroundResource(android.R.color.transparent);
                                    tag.setPadding(8, 3, 8, 3);
                                    layoutStyles.addView(tag);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
            }
        });
    }

    private void loadOrders() {
        layoutOrders.removeAllViews();
        ApiClient.getService().getBuyerOrders().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SaleRecord>>> call,
                                   Response<ApiResponse<List<SaleRecord>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SaleRecord> records = response.body().getData();
                    if (records != null && !records.isEmpty()) {
                        for (SaleRecord r : records) {
                            addOrderCard(r);
                        }
                    } else {
                        addEmpty("暂无历史订单");
                    }
                } else {
                    addEmpty("暂无历史订单");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SaleRecord>>> call, Throwable t) {
                addEmpty("网络错误，无法加载订单");
            }
        });
    }

    private void addOrderCard(SaleRecord r) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_sale_record, layoutOrders, false);
        if (card != null) {
            ((TextView) card.findViewById(R.id.tv_record_product))
                    .setText("商家订单 · " + (r.itemCount > 0 ? r.itemCount + "件" : ""));
            ((TextView) card.findViewById(R.id.tv_record_info))
                    .setText(r.saleTime != null ? r.saleTime : "");
            ((TextView) card.findViewById(R.id.tv_record_amount))
                    .setText("¥" + String.format("%.2f", r.totalAmount));
            layoutOrders.addView(card);
        }
    }

    private void addEmpty(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF95A5A6);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 16, 0, 16);
        layoutOrders.addView(tv);
    }
}
