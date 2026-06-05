package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.activity.LoginActivity;
import com.heben.client.util.TokenManager;

public class BuyerProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_profile);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView tvName = findViewById(R.id.tv_buyer_name);
        TextView tvPhone = findViewById(R.id.tv_buyer_phone);
        MaterialButton btnLogout = findViewById(R.id.btn_logout);

        String nickname = TokenManager.getNickname();
        if (nickname != null && !nickname.isEmpty()) {
            tvName.setText(nickname);
        }
        String username = TokenManager.getUsername();
        if (username != null) {
            tvPhone.setText(username);
        }

        btnLogout.setOnClickListener(v -> {
            TokenManager.logout();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }
}
