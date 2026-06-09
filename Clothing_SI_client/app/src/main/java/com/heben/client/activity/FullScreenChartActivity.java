package com.heben.client.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.local.sync.ChartDataHolder;
import com.heben.client.view.ChartView;

public class FullScreenChartActivity extends AppCompatActivity {

    public static final String EXTRA_CHART_TYPE = "chart_type";
    public static final String EXTRA_TITLE = "chart_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 强制横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_full_screen_chart);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            ChartDataHolder.clear();
            finish();
        });

        String chartType = getIntent().getStringExtra(EXTRA_CHART_TYPE);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(title != null ? title : "图表");

        ChartView chartView = findViewById(R.id.chart_view);

        if (chartType != null) {
            switch (chartType) {
                case "top8":
                    if (ChartDataHolder.topProducts != null && !ChartDataHolder.topProducts.isEmpty()) {
                        chartView.setChartData(ChartView.TYPE_BAR, ChartDataHolder.topProducts);
                    }
                    break;
                case "trend":
                    if (ChartDataHolder.dailyTrends != null && !ChartDataHolder.dailyTrends.isEmpty()) {
                        chartView.setChartData(ChartView.TYPE_LINE, ChartDataHolder.dailyTrends);
                    }
                    break;
                case "peak":
                    if (ChartDataHolder.peakHours != null && !ChartDataHolder.peakHours.isEmpty()) {
                        chartView.setChartData(ChartView.TYPE_BAR_VERTICAL, ChartDataHolder.peakHours);
                    }
                    break;
                case "profile":
                    if (ChartDataHolder.customerProfiles != null && !ChartDataHolder.customerProfiles.isEmpty()) {
                        chartView.setChartData(ChartView.TYPE_PIE, ChartDataHolder.customerProfiles);
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        ChartDataHolder.clear();
        super.onBackPressed();
    }
}
