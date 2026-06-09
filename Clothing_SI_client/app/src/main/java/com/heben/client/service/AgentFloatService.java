package com.heben.client.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.heben.client.R;
import com.heben.client.agent.AgentRuleEngine;
import com.heben.client.agent.AgentSuggestionManager;
import com.heben.client.util.TokenManager;

import java.util.List;

/**
 * 智能体全局悬浮窗服务
 * PRD要求：
 * - 半透明白色圆形(48dp)，贴边显示
 * - 可拖拽
 * - 8秒无操作自动隐藏
 * - 点击展开输入面板(280dp)
 * - 不在登录/注册页显示
 */
public class AgentFloatService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams floatParams, panelParams;

    private FrameLayout floatView;   // 圆形悬浮球
    private FrameLayout panelView;   // 展开的输入面板
    private boolean isPanelShowing = false;

    private Handler autoHideHandler = new Handler();
    private Runnable autoHideRunnable = () -> hidePanel();

    private static final int FLOAT_SIZE = 56;  // 48dp → 56px近似
    private static final int PANEL_WIDTH = 700; // 280dp → 约700px
    private static final long AUTO_HIDE_DELAY = 8000;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showFloatBall();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeAllViews();
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }

    // ===== 1. 悬浮球 =====
    private void showFloatBall() {
        floatView = (FrameLayout) LayoutInflater.from(this)
                .inflate(R.layout.view_agent_float, null);

        floatParams = new WindowManager.LayoutParams(
                dp2px(FLOAT_SIZE), dp2px(FLOAT_SIZE),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        floatParams.gravity = Gravity.START | Gravity.TOP;
        floatParams.x = 0;
        floatParams.y = dp2px(200); // 纵坐标200dp

        // 拖拽逻辑
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float touchX, touchY;
            private boolean isDragging = false;
            private long clickTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = floatParams.x;
                        initialY = floatParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        clickTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - touchX;
                        float dy = event.getRawY() - touchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true;
                            floatParams.x = (int) (initialX + dx);
                            floatParams.y = (int) (initialY + dy);
                            wm.updateViewLayout(floatView, floatParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging && System.currentTimeMillis() - clickTime < 300) {
                            // 点击 → 展开面板
                            togglePanel();
                        } else {
                            // 拖拽结束 → 贴边
                            snapToEdge();
                        }
                        return true;
                }
                return false;
            }
        });

        // 显示默认建议气泡（未读数字角标已废弃，用建议文字）
        FloatBadge badge = new FloatBadge();

        wm.addView(floatView, floatParams);
    }

    /** 贴边逻辑 */
    private void snapToEdge() {
        int screenWidth = wm.getDefaultDisplay().getWidth();
        // 靠近哪边贴哪边
        if (floatParams.x + dp2px(FLOAT_SIZE) / 2 > screenWidth / 2) {
            floatParams.x = screenWidth - dp2px(FLOAT_SIZE);
        } else {
            floatParams.x = 0;
        }
        wm.updateViewLayout(floatView, floatParams);
    }

    // ===== 2. 展开面板 =====
    private void showPanel() {
        if (panelView != null) return;
        isPanelShowing = true;

        panelView = (FrameLayout) LayoutInflater.from(this)
                .inflate(R.layout.view_agent_panel, null);

        panelParams = new WindowManager.LayoutParams(
                dp2px(280),
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.START | Gravity.TOP;

        // 面板显示在悬浮球旁边
        int ballRight = floatParams.x + dp2px(FLOAT_SIZE);
        if (ballRight + dp2px(280) > wm.getDefaultDisplay().getWidth()) {
            // 向左展开
            panelParams.x = floatParams.x - dp2px(280);
        } else {
            panelParams.x = ballRight + dp2px(4);
        }
        panelParams.y = floatParams.y;

        // 设置输入框和发送
        EditText etInput = panelView.findViewById(R.id.et_agent_input);
        View btnSend = panelView.findViewById(R.id.btn_agent_send);
        View btnFull = panelView.findViewById(R.id.btn_agent_full);

        // 让输入框可聚焦
        panelView.setFocusable(true);
        panelView.setFocusableInTouchMode(true);

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                // 本地规则引擎先处理
                AgentRuleEngine.ParseResult result = AgentRuleEngine.parse(text);
                if (result.confidence >= 0.85) {
                    showFloatingConfirm(result);
                }
                etInput.setText("");
                // 短时显示面板不消失，但重置自动隐藏
                resetAutoHide();
            }
        });

        btnFull.setOnClickListener(v -> {
            // 打开完整面板（已迁移到底部弹窗，此入口废弃）
            hidePanel();
        });

        // 关闭按钮
        panelView.findViewById(R.id.btn_agent_close).setOnClickListener(v -> hidePanel());

        wm.addView(panelView, panelParams);

        // 自动聚焦输入框
        etInput.postDelayed(() -> {
            etInput.requestFocus();
            // 需要弹出键盘则依赖FLAG_NOT_FOCUSABLE的移除
            panelView.setFocusableInTouchMode(true);
            etInput.setFocusableInTouchMode(true);
            etInput.requestFocus();
        }, 200);

        resetAutoHide();
    }

    private void hidePanel() {
        if (panelView != null) {
            try { wm.removeView(panelView); } catch (Exception ignored) {}
            panelView = null;
        }
        isPanelShowing = false;
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }

    private void togglePanel() {
        if (isPanelShowing) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void resetAutoHide() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
    }

    /** 浮动确认弹窗（仅在展开面板时调用） */
    private void showFloatingConfirm(AgentRuleEngine.ParseResult result) {
        // 创建一个覆盖在面板上的确认弹窗
        FrameLayout confirmView = (FrameLayout) LayoutInflater.from(this)
                .inflate(R.layout.view_agent_confirm, null);

        WindowManager.LayoutParams confirmParams = new WindowManager.LayoutParams(
                dp2px(260),
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        confirmParams.gravity = Gravity.CENTER;
        confirmParams.x = 0;
        confirmParams.y = dp2px(-60);

        TextView tvMessage = confirmView.findViewById(R.id.tv_confirm_message);
        tvMessage.setText(result.displayText);

        confirmView.findViewById(R.id.btn_confirm_yes).setOnClickListener(v -> {
            executeAgentAction(result);
            try { wm.removeView(confirmView); } catch (Exception ignored) {}
            hidePanel();
        });

        confirmView.findViewById(R.id.btn_confirm_no).setOnClickListener(v -> {
            try { wm.removeView(confirmView); } catch (Exception ignored) {}
        });

        wm.addView(confirmView, confirmParams);

        // 3秒后自动消失
        new Handler().postDelayed(() -> {
            try { wm.removeView(confirmView); } catch (Exception ignored) {}
        }, 3000);
    }

    /** 执行智能体指令 */
    private void executeAgentAction(AgentRuleEngine.ParseResult result) {
        // 根据action类型执行对应操作
        switch (result.action) {
            case "open_page": {
                String page = result.params.get("arg1").toString();
                Intent intent;
                String p = page.contains("经营") || page.contains("分析") || page.contains("数据") ? "ANALYTICS"
                        : page.contains("商品") || page.contains("产品") || page.contains("管理") ? "PRODUCTS"
                        : page.contains("分类") || page.contains("类目") ? "CATEGORIES"
                        : page.contains("进货") || page.contains("入库") || page.contains("采购") ? "PURCHASE"
                        : page.contains("订单") || page.contains("销售") || page.contains("开单") ? "SALE"
                        : page.contains("预约") || page.contains("日程") ? "APPOINTMENTS"
                        : page.contains("属性") || page.contains("尺码") || page.contains("颜色") ? "ATTRIBUTES"
                        : page.contains("我") || page.contains("个人") || page.contains("设置") ? "PROFILE"
                        : page.contains("共享") || page.contains("批发") || page.contains("协作") ? "SHARED"
                        : "";
                // 跳转到对应页面
                Class<?> target = null;
                try {
                    if (p.equals("ANALYTICS")) target = Class.forName("com.heben.client.activity.AnalyticsActivity");
                    else if (p.equals("PRODUCTS")) target = Class.forName("com.heben.client.activity.ProductsActivity");
                    else if (p.equals("CATEGORIES")) target = Class.forName("com.heben.client.activity.CategoriesActivity");
                    else if (p.equals("PURCHASE")) target = Class.forName("com.heben.client.activity.PurchaseActivity");
                    else if (p.equals("SALE")) target = Class.forName("com.heben.client.activity.SaleActivity");
                    else if (p.equals("APPOINTMENTS")) target = Class.forName("com.heben.client.activity.AppointmentsActivity");
                    else if (p.equals("ATTRIBUTES")) target = Class.forName("com.heben.client.activity.AttributesActivity");
                    else if (p.equals("SHARED")) target = Class.forName("com.heben.client.activity.SharedProductsActivity");
                } catch (ClassNotFoundException ignored) {}
                if (target != null) {
                    Intent i = new Intent(this, target);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                break;
            }
            case "system_setting": {
                // 声音/通知设置通过SharedPreferences存储
                String action = result.params.get("arg1").toString();
                String target = result.params.get("arg2").toString();
                boolean enable = action.contains("打开") || action.contains("开启");
                String key = target.contains("声音") || target.contains("音效") ? "setting_sound"
                        : target.contains("通知") || target.contains("提示音") ? "setting_notification"
                        : "setting_vibrate";
                getSharedPreferences("agent_settings", MODE_PRIVATE)
                        .edit().putBoolean(key, enable).apply();
                break;
            }
            case "stock_query": {
                // 打开商品页面并过滤低库存
                Intent i = new Intent(this, com.heben.client.activity.ProductsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("filter_low_stock", true);
                startActivity(i);
                break;
            }
            case "query_sales": {
                // 打开分析页
                Intent i = new Intent(this, com.heben.client.activity.AnalyticsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
            }
            default:
                // 其他操作已迁移到底部弹窗，悬浮窗不再处理
                break;
        }
    }

    private void removeAllViews() {
        if (floatView != null) {
            try { wm.removeView(floatView); } catch (Exception ignored) {}
            floatView = null;
        }
        if (panelView != null) {
            try { wm.removeView(panelView); } catch (Exception ignored) {}
            panelView = null;
        }
    }

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 悬浮球上的未读/建议指示 */
    private class FloatBadge {
        View badgeView;
        FloatBadge() {
            badgeView = floatView.findViewById(R.id.tv_float_badge);
            if (badgeView != null) {
                List<AgentSuggestionManager.Suggestion> suggestions = AgentSuggestionManager.getSuggestions();
                if (!suggestions.isEmpty()) {
                    badgeView.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
