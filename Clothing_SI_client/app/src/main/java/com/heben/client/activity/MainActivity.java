package com.heben.client.activity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.heben.client.R;
import com.heben.client.fragment.BillsFragment;
import com.heben.client.fragment.HomeFragment;
import com.heben.client.fragment.MessagesFragment;
import com.heben.client.fragment.ProfileFragment;
import com.heben.client.local.DataSyncManager;
import com.heben.client.util.SoundManager;
import com.heben.client.widget.AgentBottomSheetDialog;

public class MainActivity extends AppCompatActivity {

    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new DataSyncManager().checkAndSync();

        // 默认显示首页
        if (savedInstanceState == null) {
            activeFragment = new HomeFragment();
            loadFragment(activeFragment);
            // 首页默认选中态已在 XML 中预设，但 Java 侧也要同步
            setTabActive(R.id.nav_home);
        }

        // 自定义底部导航点击
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(new HomeFragment(), R.id.nav_home);
        });
        findViewById(R.id.nav_bills).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(new BillsFragment(), R.id.nav_bills);
        });
        findViewById(R.id.nav_messages).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(new MessagesFragment(), R.id.nav_messages);
        });
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(new ProfileFragment(), R.id.nav_profile);
        });

        // 导航栏居中 🤖 → 弹出智能体卡片弹窗
        findViewById(R.id.nav_agent).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            new AgentBottomSheetDialog().show(getSupportFragmentManager(), "agent_sheet");
        });
    }

    private void switchTab(Fragment fragment, int tabId) {
        if (fragment.getClass().equals(activeFragment != null ? activeFragment.getClass() : null)) return;
        activeFragment = fragment;
        loadFragment(fragment);
        setTabActive(tabId);
    }

    /** 统一设置 tab 选中态：icon 加 container 背景、文字加粗变色 */
    private void setTabActive(int activeTabId) {
        int[] tabIds = {R.id.nav_home, R.id.nav_bills, R.id.nav_messages, R.id.nav_profile};
        int[] iconIds = {R.id.nav_home_icon, R.id.nav_bills_icon, R.id.nav_messages_icon, R.id.nav_profile_icon};
        int[] labelIds = {R.id.nav_home_label, R.id.nav_bills_label, R.id.nav_messages_label, R.id.nav_profile_label};

        int primaryColor = ContextCompat.getColor(this, R.color.md_primary);
        int variantColor = ContextCompat.getColor(this, R.color.md_on_surface_variant);
        ColorStateList containerTint = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.md_primary_container));

        for (int i = 0; i < tabIds.length; i++) {
            boolean isActive = tabIds[i] == activeTabId;

            TextView icon = findViewById(iconIds[i]);
            TextView label = findViewById(labelIds[i]);
            if (icon == null || label == null) continue;

            if (isActive) {
                icon.setBackgroundResource(R.drawable.circle_bg);
                icon.setBackgroundTintList(containerTint);
                label.setTextColor(primaryColor);
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                icon.setBackgroundResource(android.R.color.transparent);
                icon.setBackgroundTintList(null);
                label.setTextColor(variantColor);
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }
}
