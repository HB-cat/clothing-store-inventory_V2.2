package com.heben.client.activity;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.*;
import com.heben.client.network.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsActivity extends AppCompatActivity {

    private LinearLayout layoutLowStock, layoutSlowMoving, layoutTopProducts;
    private TextView tvConclusion, tvDetail, tvLowStockTitle, tvSlowMovingTitle, tvTrend, tvPeakHours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvConclusion = findViewById(R.id.tv_today_conclusion);
        tvDetail = findViewById(R.id.tv_today_detail);
        layoutLowStock = findViewById(R.id.layout_low_stock);
        layoutSlowMoving = findViewById(R.id.layout_slow_moving);
        layoutTopProducts = findViewById(R.id.layout_top_products);
        tvLowStockTitle = findViewById(R.id.tv_low_stock_title);
        tvSlowMovingTitle = findViewById(R.id.tv_slow_moving_title);
        tvTrend = findViewById(R.id.tv_trend);
        tvPeakHours = findViewById(R.id.tv_peak_hours);

        loadData();
    }

    private void loadData() {
        loadTodayReport();
        loadLowStock();
        loadSlowMoving();
        loadTopProducts();
        loadTrend();
        loadPeakHours();
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
                        tvLowStockTitle.setText("⚠️ 补货建议 (" + list.size() + "件商品库存不足)");
                        for (LowStockProduct p : list) {
                            TextView tv = newTextView(p.isEmergency ? "🔴 " : "🟡 " +
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
                                    " · 库存 " + p.stock + " 件 · 30天销量 " + p.sales30Day));
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
                layoutTopProducts.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TopProduct> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < list.size(); i++) {
                            TopProduct p = list.get(i);
                            sb.append(i + 1).append(". ").append(p.productName)
                                    .append(" · 销量").append(p.salesCount)
                                    .append(" · ¥").append(String.format("%.1f", p.salesAmount));
                            if (i < list.size() - 1) sb.append("\n");
                        }
                        layoutTopProducts.addView(newTextView(sb.toString()));
                    } else {
                        layoutTopProducts.addView(newTextView("暂无销售数据"));
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
                        StringBuilder sb = new StringBuilder();
                        int max = Math.min(list.size(), 7);
                        for (int i = 0; i < max; i++) {
                            DailyTrend d = list.get(i);
                            sb.append(d.date).append(" ¥").append(String.format("%.0f", d.amount));
                            if (i < max - 1) sb.append("\n");
                        }
                        tvTrend.setText(sb.toString());
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
                        StringBuilder sb = new StringBuilder();
                        int max = Math.min(list.size(), 5);
                        for (int i = 0; i < max; i++) {
                            PeakHour p = list.get(i);
                            sb.append(p.hour).append(":00-").append(p.hour + 1).append(":00")
                                    .append(" · ").append(p.orderCount).append("单");
                            if (i < max - 1) sb.append("\n");
                        }
                        tvPeakHours.setText(sb.toString());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PeakHour>>> call, Throwable t) {}
        });
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
