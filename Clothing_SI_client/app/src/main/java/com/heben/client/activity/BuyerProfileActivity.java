package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

        // 📋 我的订单
        findViewById(R.id.btn_orders).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerOrdersActivity.class)));

        // 📅 我的预约 → 跳转到消息页并切到预约 tab
        findViewById(R.id.btn_appointments).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerMessagesActivity.class)
                        .putExtra("switch_to_appointment", true)));

        // ❤️ 收藏店铺
        findViewById(R.id.btn_favorites).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerFollowsActivity.class)));

        // 📖 使用指南
        findViewById(R.id.btn_guide).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerGuideActivity.class)));

        // 📋 客户信息
        findViewById(R.id.btn_info_edit).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerInfoEditActivity.class)));

        // 🚪 退出登录
        findViewById(R.id.btn_logout).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("🚪 退出登录")
                        .setMessage("确定要退出登录吗？")
                        .setPositiveButton("确定退出", (d, w) -> {
                            TokenManager.logout();
                            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finishAffinity();
                        })
                        .setNegativeButton("取消", null)
                        .show()
        );
    }
}
