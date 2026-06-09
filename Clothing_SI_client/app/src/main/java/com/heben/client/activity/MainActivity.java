package com.heben.client.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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

        // 更新 Tab 文字颜色（只更新4个页面 tab，不包括 agent）
        int[] tabIds = {R.id.nav_home, R.id.nav_bills, R.id.nav_messages, R.id.nav_profile};
        for (int id : tabIds) {
            View tab = findViewById(id);
            if (tab instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout ll = (android.widget.LinearLayout) tab;
                if (ll.getChildCount() > 1 && ll.getChildAt(1) instanceof TextView) {
                    ((TextView) ll.getChildAt(1)).setTextColor(id == tabId ? 0xFF2E7D32 : 0xFF5D6D7E);
                }
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
