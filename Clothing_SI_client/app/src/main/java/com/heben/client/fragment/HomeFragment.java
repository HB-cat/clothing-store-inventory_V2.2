package com.heben.client.fragment;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.heben.client.R;
import com.heben.client.activity.*;
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.widget.AgentBottomSheetDialog;

public class HomeFragment extends Fragment {

    private static final int MARQUEE_SPEED = 50; // px per second, consistent speed

    private ValueAnimator marqueeAnim;
    private HorizontalScrollView marqueeScroll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        v.findViewById(R.id.btn_purchase).setOnClickListener(x -> startActivity(new Intent(getActivity(), PurchaseActivity.class)));
        v.findViewById(R.id.btn_new_order).setOnClickListener(x -> startActivity(new Intent(getActivity(), NewOrderActivity.class)));
        v.findViewById(R.id.btn_sale).setOnClickListener(x -> startActivity(new Intent(getActivity(), SaleActivity.class)));
        v.findViewById(R.id.todo_card).setOnClickListener(x -> startActivity(new Intent(getActivity(), NewOrderActivity.class)));

        // 快捷指令 → 弹出智能体卡片弹窗（不跳转新界面）
        v.findViewById(R.id.et_quick_cmd).setOnClickListener(x -> showAgentSheet());
        v.findViewById(R.id.et_quick_cmd).setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.clearFocus();
                showAgentSheet();
            }
        });

        // 跑马灯 — 5 个 TextView + HorizontalScrollView.scrollX 动画
        String msg = "🎵 天天发财 · 客似云来 · 生意兴隆    ";
        TextView tv1 = v.findViewById(R.id.tv_marquee_1);
        TextView tv2 = v.findViewById(R.id.tv_marquee_2);
        TextView tv3 = v.findViewById(R.id.tv_marquee_3);
        TextView tv4 = v.findViewById(R.id.tv_marquee_4);
        TextView tv5 = v.findViewById(R.id.tv_marquee_5);

        if (!NetworkUtil.isConnected(getActivity())) {
            tv1.setText(msg.trim());
            tv2.setVisibility(View.GONE);
            tv3.setVisibility(View.GONE);
            tv4.setVisibility(View.GONE);
            tv5.setVisibility(View.GONE);
        } else {
            tv1.setText(msg);
            tv2.setText(msg);
            tv3.setText(msg);
            tv4.setText(msg);
            tv5.setText(msg);
            marqueeScroll = v.findViewById(R.id.marquee_scroll);
            marqueeScroll.post(() -> startMarquee());
        }

        return v;
    }

    private void startMarquee() {
        View child = marqueeScroll.getChildAt(0);
        if (child == null) return;
        // child is the LinearLayout containing 5 TextViews
        int contentWidth = child.getWidth();
        if (contentWidth == 0) {
            marqueeScroll.post(() -> startMarquee());
            return;
        }
        int copyWidth = contentWidth / 5; // one copy width
        marqueeScroll.scrollTo(0, 0);

        long duration = (long) ((float) copyWidth / MARQUEE_SPEED * 1000);
        marqueeAnim = ValueAnimator.ofInt(0, copyWidth);
        marqueeAnim.setDuration(duration);
        marqueeAnim.setRepeatCount(ValueAnimator.INFINITE);
        marqueeAnim.setRepeatMode(ValueAnimator.RESTART);
        marqueeAnim.addUpdateListener(anim ->
            marqueeScroll.scrollTo((Integer) anim.getAnimatedValue(), 0));
        marqueeAnim.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (marqueeScroll != null && (marqueeAnim == null || !marqueeAnim.isRunning()) && NetworkUtil.isConnected(getActivity())) {
            marqueeScroll.post(() -> startMarquee());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (marqueeAnim != null) {
            marqueeAnim.cancel();
            marqueeAnim = null;
        }
    }

    private void showAgentSheet() {
        new AgentBottomSheetDialog().show(getParentFragmentManager(), "agent_sheet");
    }
}
