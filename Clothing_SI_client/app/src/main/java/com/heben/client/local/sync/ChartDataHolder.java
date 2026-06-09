package com.heben.client.local.sync;

import com.heben.client.model.Models.CustomerProfile;
import com.heben.client.model.Models.DailyTrend;
import com.heben.client.model.Models.PeakHour;
import com.heben.client.model.Models.TopProduct;

import java.util.List;

/**
 * 静态数据桥 — 将 AnalyticsActivity 已加载的图表数据传递给 FullScreenChartActivity
 */
public class ChartDataHolder {
    public static List<TopProduct> topProducts;
    public static List<DailyTrend> dailyTrends;
    public static List<PeakHour> peakHours;
    public static List<CustomerProfile> customerProfiles;

    public static void clear() {
        topProducts = null;
        dailyTrends = null;
        peakHours = null;
        customerProfiles = null;
    }
}
