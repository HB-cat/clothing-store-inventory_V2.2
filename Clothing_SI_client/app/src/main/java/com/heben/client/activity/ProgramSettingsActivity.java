package com.heben.client.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.local.sync.SyncEngine;
import com.heben.client.util.SoundManager;

public class ProgramSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_settings);
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        SharedPreferences prefs = getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);

        // 同步数据到云端
        findViewById(R.id.settings_sync).setOnClickListener(v -> {
            if (!NetworkUtil.isConnected(this)) {
                Toast.makeText(this, "无网络连接，无法同步", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "正在同步...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                SyncEngine engine = new SyncEngine(this);
                boolean ok = engine.syncAll();
                runOnUiThread(() ->
                    Toast.makeText(ProgramSettingsActivity.this,
                        ok ? "同步完成" : "部分同步失败，请检查网络后重试",
                        Toast.LENGTH_SHORT).show());
            }).start();
        });

        // 首页状态文字
        TextInputEditText etHomeStatus = findViewById(R.id.et_home_status);
        etHomeStatus.setText(prefs.getString("home_status_text", "🎵 天天发财 · 客似云来 · 生意兴隆"));
        findViewById(R.id.btn_save_home_status).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            String text = etHomeStatus.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "文字不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("home_status_text", text).apply();
            Toast.makeText(this, "已保存，返回首页即可看到效果", Toast.LENGTH_SHORT).show();
        });

        // 操作音效开关
        SwitchMaterial switchSound = findViewById(R.id.switch_sound);
        switchSound.setChecked(prefs.getBoolean("sound_enabled", true));
        switchSound.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("sound_enabled", checked).apply();
            SoundManager.getInstance(this).playClick();
            Toast.makeText(this, checked ? "操作音效已开启" : "操作音效已关闭", Toast.LENGTH_SHORT).show();
        });

        // 通知开关（占位，实际由后端控制）
        SwitchMaterial switchAgent = findViewById(R.id.switch_notify_agent);
        switchAgent.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, checked ? "智能体建议已开启" : "智能体建议已关闭", Toast.LENGTH_SHORT).show());

        SwitchMaterial switchAppt = findViewById(R.id.switch_notify_appt);
        switchAppt.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, checked ? "预约通知已开启" : "预约通知已关闭", Toast.LENGTH_SHORT).show());

        SwitchMaterial switchChat = findViewById(R.id.switch_notify_chat);
        switchChat.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(this, checked ? "聊天通知已开启" : "聊天通知已关闭", Toast.LENGTH_SHORT).show());
    }
}
