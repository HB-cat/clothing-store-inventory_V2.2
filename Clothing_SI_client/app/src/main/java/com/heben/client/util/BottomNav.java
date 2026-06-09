package com.heben.client.util;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.heben.client.R;
import com.heben.client.activity.BuyerFollowsActivity;
import com.heben.client.activity.BuyerHomeActivity;
import com.heben.client.activity.BuyerMessagesActivity;
import com.heben.client.activity.BuyerProfileActivity;

/**
 * Buyer 端底部导航统一设置
 * 各 Activity 在 onCreate 中调用 BottomNav.setup(this, R.id.bottom_home)
 * 传入当前 tab 的 id 即可。
 */
public class BottomNav {

    public static void setup(Activity activity, int activeTabId) {
        View root = activity.findViewById(android.R.id.content).getRootView();
        View home = root.findViewById(R.id.bottom_home);
        View follow = root.findViewById(R.id.bottom_follow);
        View messages = root.findViewById(R.id.bottom_messages);
        View profile = root.findViewById(R.id.bottom_profile);

        // 高亮当前 tab
        int selectedColor = activity.getResources().getColor(R.color.primary, null);
        int normalColor = 0xFF95A5A6;

        if (home != null) {
            ((TextView) home).setTextColor(home.getId() == activeTabId ? selectedColor : normalColor);
            ((TextView) home).setTypeface(null, home.getId() == activeTabId ?
                    android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            home.setOnClickListener(v -> {
                if (home.getId() != activeTabId)
                    activity.startActivity(new Intent(activity, BuyerHomeActivity.class));
            });
        }
        if (follow != null) {
            ((TextView) follow).setTextColor(follow.getId() == activeTabId ? selectedColor : normalColor);
            ((TextView) follow).setTypeface(null, follow.getId() == activeTabId ?
                    android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            follow.setOnClickListener(v -> {
                if (follow.getId() != activeTabId)
                    activity.startActivity(new Intent(activity, BuyerFollowsActivity.class));
            });
        }
        if (messages != null) {
            ((TextView) messages).setTextColor(messages.getId() == activeTabId ? selectedColor : normalColor);
            ((TextView) messages).setTypeface(null, messages.getId() == activeTabId ?
                    android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            messages.setOnClickListener(v -> {
                if (messages.getId() != activeTabId)
                    activity.startActivity(new Intent(activity, BuyerMessagesActivity.class));
            });
        }
        if (profile != null) {
            ((TextView) profile).setTextColor(profile.getId() == activeTabId ? selectedColor : normalColor);
            ((TextView) profile).setTypeface(null, profile.getId() == activeTabId ?
                    android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            profile.setOnClickListener(v -> {
                if (profile.getId() != activeTabId)
                    activity.startActivity(new Intent(activity, BuyerProfileActivity.class));
            });
        }
    }
}
