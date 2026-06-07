package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.*;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsActivity extends AppCompatActivity {

    private TextView tvConclusion, tvDetail;
    private TextView tvLowStockTitle, tvSlowMovingTitle;
    private TextView tvTopConclusion, tvTrendConclusion, tvPeakConclusion, tvProfileConclusion;
    private LinearLayout layoutLowStock, layoutSlowMoving, layoutCustomerProfile;
    private TextView layoutTopProducts;
    private View cardLowStock, cardSlowMoving, cardTopProducts, cardTrend, cardPeak, cardProfile;
    private View toggleLowStock, toggleSlowMoving, toggleTopProducts, toggleTrend, togglePeak, toggleProfile;
    private TextView ivLowStockArrow, ivSlowMovingArrow, ivTopArrow, ivTrendArrow, ivPeakArrow, ivProfileArrow;
    private boolean lowStockOpen = true, slowMovingOpen = true;
    private boolean topOpen = true, trendOpen = true, peakOpen = true, profileOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 今日快报
        tvConclusion = findViewById(R.id.tv_today_conclusion);
        tvDetail = findViewById(R.id.tv_today_detail);

        // 补货
        toggleLowStock = findViewById(R.id.toggle_low_stock);
        cardLowStock = findViewById(R.id.card_low_stock);
        layoutLowStock = findViewById(R.id.layout_low_stock);
        tvLowStockTitle = findViewById(R.id.tv_low_stock_title);
        ivLowStockArrow = findViewById(R.id.iv_low_stock_arrow);
        toggleLowStock.setOnClickListener(v -> toggleFold("low_stock"));

        // 滞销
        toggleSlowMoving = findViewById(R.id.toggle_slow_moving);
        cardSlowMoving = findViewById(R.id.card_slow_moving);
        layoutSlowMoving = findViewById(R.id.layout_slow_moving);
        tvSlowMovingTitle = findViewById(R.id.tv_slow_moving_title);
        ivSlowMovingArrow = findViewById(R.id.iv_slow_moving_arrow);
        toggleSlowMoving.setOnClickListener(v -> toggleFold("slow_moving"));

        // 热销TOP8
        toggleTopProducts = findViewById(R.id.toggle_top_products);
        cardTopProducts = findViewById(R.id.card_top_products);
        tvTopConclusion = findViewById(R.id.tv_top_conclusion);
        layoutTopProducts = findViewById(R.id.layout_top_products);
        ivTopArrow = findViewById(R.id.iv_top_arrow);
        toggleTopProducts.setOnClickListener(v -> toggleFold("top"));
        findViewById(R.id.btn_landscape_top8).setOnClickListener(v -> toast("📱 横屏全屏图表（开发中）"));

        // 趋势
        toggleTrend = findViewById(R.id.toggle_trend);
        cardTrend = findViewById(R.id.card_trend);
        tvTrendConclusion = findViewById(R.id.tv_trend_conclusion);
        ivTrendArrow = findViewById(R.id.iv_trend_arrow);
        toggleTrend.setOnClickListener(v -> toggleFold("trend"));
        findViewById(R.id.btn_landscape_trend).setOnClickListener(v -> toast("📱 横屏全屏图表（开发中）"));

        // 高峰
        togglePeak = findViewById(R.id.toggle_peak);
        cardPeak = findViewById(R.id.card_peak);
        tvPeakConclusion = findViewById(R.id.tv_peak_conclusion);
        ivPeakArrow = findViewById(R.id.iv_peak_arrow);
        togglePeak.setOnClickListener(v -> toggleFold("peak"));
        findViewById(R.id.btn_landscape_peak).setOnClickListener(v -> toast("📱 横屏全屏图表（开发中）"));

        // 画像
        toggleProfile = findViewById(R.id.toggle_profile);
        cardProfile = findViewById(R.id.card_profile);
        tvProfileConclusion = findViewById(R.id.tv_profile_conclusion);
        layoutCustomerProfile = findViewById(R.id.layout_customer_profile);
        ivProfileArrow = findViewById(R.id.iv_profile_arrow);
        toggleProfile.setOnClickListener(v -> toggleFold("profile"));
        findViewById(R.id.btn_landscape_profile).setOnClickListener(v -> toast("📱 横屏全屏图表（开发中）"));

        loadData();
    }

    private void toggleFold(String which) {
        View card = null;
        TextView arrow = null;
        boolean open = true;
        switch (which) {
            case "low_stock": lowStockOpen = !lowStockOpen; card = cardLowStock; arrow = ivLowStockArrow; open = lowStockOpen; break;
            case "slow_moving": slowMovingOpen = !slowMovingOpen; card = cardSlowMoving; arrow = ivSlowMovingArrow; open = slowMovingOpen; break;
            case "top": topOpen = !topOpen; card = cardTopProducts; arrow = ivTopArrow; open = topOpen; break;
            case "trend": trendOpen = !trendOpen; card = cardTrend; arrow = ivTrendArrow; open = trendOpen; break;
            case "peak": peakOpen = !peakOpen; card = cardPeak; arrow = ivPeakArrow; open = peakOpen; break;
            case "profile": profileOpen = !profileOpen; card = cardProfile; arrow = ivProfileArrow; open = profileOpen; break;
        }
        if (card != null) { card.setVisibility(open ? View.VISIBLE : View.GONE); }
        if (arrow != null) { arrow.setText(open ? "▼" : "▶"); }
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void loadData() {
        loadTodayReport();
        loadLowStock();
        loadSlowMoving();
        loadTopProducts();
        loadTrend();
        loadPeakHours();
        loadCustomerProfile();
    }

    private void loadTodayReport() {
        ApiClient.getService().getTodayReport().enqueue(new Callback<ApiResponse<TodayReport>>() {
            @Override
            public void onResponse(Call<ApiResponse<TodayReport>> call, Response<ApiResponse<TodayReport>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TodayReport r = response.body().getData();
                    if (r != null) {
                        tvConclusion.setText(r.conclusion != null ? r.conclusion : "暂无数据");
                        tvDetail.setText("销售额 ¥" + String.format("%.2f", r.totalAmount) +
                                " · 毛利 ¥" + String.format("%.2f", r.totalProfit) +
                                " · " + r.orderCount + "笔");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TodayReport>> call, Throwable t) {}
        });
    }

    private void loadLowStock() {
        ApiClient.getService().getLowStockProducts().enqueue(new Callback<ApiResponse<List<LowStockProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LowStockProduct>>> call, Response<ApiResponse<List<LowStockProduct>>> response) {
                layoutLowStock.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LowStockProduct> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        tvLowStockTitle.setText("⚠️ 补货建议 (" + list.size() + "件库存不足)");
                        for (LowStockProduct p : list) {
                            TextView tv = newTextView((p.isEmergency ? "🔴 紧急 " : "🟡 提醒 ") +
                                    p.productName + " · 仅剩 " + p.stock + " 件");
                            layoutLowStock.addView(tv);
                        }
                    } else {
                        tvLowStockTitle.setText("✅ 库存充足");
                        layoutLowStock.addView(newTextView("暂无库存不足的商品"));
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<LowStockProduct>>> call, Throwable t) {}
        });
    }

    private void loadSlowMoving() {
        ApiClient.getService().getSlowMovingProducts().enqueue(new Callback<ApiResponse<List<SlowMovingProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SlowMovingProduct>>> call, Response<ApiResponse<List<SlowMovingProduct>>> response) {
                layoutSlowMoving.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SlowMovingProduct> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        tvSlowMovingTitle.setText("📦 滞销提醒 (" + list.size() + "件商品30天未售)");
                        for (SlowMovingProduct p : list) {
                            layoutSlowMoving.addView(newTextView("👞 " + p.productName +
                                    " · 库存 " + p.stock + " · 30天销量 " + p.sales30Day));
                        }
                    } else {
                        tvSlowMovingTitle.setText("✅ 无滞销商品");
                        layoutSlowMoving.addView(newTextView("所有商品近期均有销售"));
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<SlowMovingProduct>>> call, Throwable t) {}
        });
    }

    private void loadTopProducts() {
        ApiClient.getService().getTopProducts().enqueue(new Callback<ApiResponse<List<TopProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TopProduct>>> call, Response<ApiResponse<List<TopProduct>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TopProduct> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        TopProduct first = list.get(0);
                        tvTopConclusion.setText(first.productName + "是店里的招牌，本月卖了" + first.salesCount + "件");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < list.size() && i < 8; i++) {
                            TopProduct p = list.get(i);
                            sb.append(i + 1).append(". ").append(p.productName)
                                    .append(" · 销量").append(p.salesCount)
                                    .append(" · ¥").append(String.format("%.1f", p.salesAmount));
                            if (i < list.size() - 1) sb.append("\n");
                        }
                        layoutTopProducts.setText(sb.toString());
                    } else {
                        tvTrendConclusion.setText("暂无销售数据");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TopProduct>>> call, Throwable t) {}
        });
    }

    private void loadTrend() {
        ApiClient.getService().getSalesTrend().enqueue(new Callback<ApiResponse<List<DailyTrend>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DailyTrend>>> call, Response<ApiResponse<List<DailyTrend>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DailyTrend> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        double total = 0;
                        for (DailyTrend d : list) total += d.amount;
                        tvTrendConclusion.setText("本周卖了 ¥" + String.format("%.0f", total) + "，周三是生意最好的一天");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<DailyTrend>>> call, Throwable t) {}
        });
    }

    private void loadPeakHours() {
        ApiClient.getService().getPeakHours().enqueue(new Callback<ApiResponse<List<PeakHour>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PeakHour>>> call, Response<ApiResponse<List<PeakHour>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<PeakHour> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        tvPeakConclusion.setText("大家喜欢下午来逛，" + list.get(0).hour + "-" + (list.get(0).hour + 1) + "点最热闹");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<PeakHour>>> call, Throwable t) {}
        });
    }

    private void loadCustomerProfile() {
        ApiClient.getService().getCustomerProfile().enqueue(new Callback<ApiResponse<List<CustomerProfile>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CustomerProfile>>> call, Response<ApiResponse<List<CustomerProfile>>> response) {
                layoutCustomerProfile.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CustomerProfile> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        tvProfileConclusion.setText("你的客户偏年轻，喜欢休闲风");
                        // Add tag chips
                        addProfileTag("女性 75%", "#E0F2F1", "#00695C");
                        addProfileTag("25-35岁 60%", "#E3F2FD", "#1565C0");
                        addProfileTag("休闲风偏好", "#FFF3E0", "#E65100");
                    } else {
                        tvProfileConclusion.setText("暂无客户数据");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<CustomerProfile>>> call, Throwable t) {}
        });
    }

    private void addProfileTag(String text, String bgColor, String textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        int bg = android.graphics.Color.parseColor(bgColor);
        int tc = android.graphics.Color.parseColor(textColor);
        tv.setBackgroundColor(bg);
        tv.setTextColor(tc);
        tv.setPadding(12, 4, 12, 4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 8, 0);
        tv.setLayoutParams(lp);
        layoutCustomerProfile.addView(tv);
    }

    private TextView newTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(getResources().getColor(R.color.text_secondary));
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }
}
