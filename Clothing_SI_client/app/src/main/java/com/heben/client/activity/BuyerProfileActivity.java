package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.util.BottomNav;
import com.heben.client.util.TokenManager;

public class BuyerProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_profile);

        BottomNav.setup(this, R.id.bottom_profile);

        TextView tvName = findViewById(R.id.tv_buyer_name);
        TextView tvInfo = findViewById(R.id.tv_buyer_info);

        tvName.setText(TokenManager.getNickname() != null ? TokenManager.getNickname() : "买家");
        tvInfo.setText("买家 · 欢迎使用");

        findViewById(R.id.btn_orders).setOnClickListener(v ->
                Toast.makeText(this, "订单功能开发中", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_appointments).setOnClickListener(v ->
                Toast.makeText(this, "预约功能开发中", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_favorites).setOnClickListener(v -> {
            Intent intent = new Intent(this, BuyerFollowsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            TokenManager.logout();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }
}
