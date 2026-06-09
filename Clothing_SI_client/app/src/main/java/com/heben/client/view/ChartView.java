package com.heben.client.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.heben.client.model.Models.CustomerProfile;
import com.heben.client.model.Models.DailyTrend;
import com.heben.client.model.Models.PeakHour;
import com.heben.client.model.Models.TopProduct;

import java.util.Collections;
import java.util.List;

/**
 * 自定义图表 View — 支持柱状图 / 折线图 / 饼图
 * 在 FullScreenChartActivity 的横屏布局中使用
 */
public class ChartView extends View {

    public static final int TYPE_BAR = 0;
    public static final int TYPE_LINE = 1;
    public static final int TYPE_PIE = 2;
    public static final int TYPE_BAR_VERTICAL = 3;

    private int chartType = TYPE_BAR;
    private List<?> data;

    private final Paint paintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPie = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTitle = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] PIE_COLORS = {
            0xFF2E7D32, 0xFF43A047, 0xFF66BB6A, 0xFF81C784,
            0xFFA5D6A7, 0xFFC8E6C9, 0xFF1B5E20, 0xFF388E3C
    };
    private static final int BAR_COLOR = 0xFF2E7D32;
    private static final int LINE_COLOR = 0xFF2E7D32;
    private static final int GRID_COLOR = 0xFFE0E0E0;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintBar.setColor(BAR_COLOR);
        paintBar.setStyle(Paint.Style.FILL);

        paintLine.setColor(LINE_COLOR);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(4f);
        paintLine.setStrokeCap(Paint.Cap.ROUND);
        paintLine.setStrokeJoin(Paint.Join.ROUND);

        paintText.setColor(0xFF5D6D7E);
        paintText.setTextSize(32f);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintTitle.setColor(0xFF1B1F1B);
        paintTitle.setTextSize(42f);
        paintTitle.setTextAlign(Paint.Align.LEFT);
        paintTitle.setFakeBoldText(true);

        paintPie.setStyle(Paint.Style.FILL);

        paintGrid.setColor(GRID_COLOR);
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(1f);
    }

    public void setChartData(int type, List<?> data) {
        this.chartType = type;
        this.data = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) {
            paintText.setTextSize(40f);
            paintText.setColor(0xFF9CA3AF);
            canvas.drawText("暂无数据", getWidth() / 2f, getHeight() / 2f, paintText);
            return;
        }

        switch (chartType) {
            case TYPE_BAR:
                drawBarChart(canvas);
                break;
            case TYPE_LINE:
                drawLineChart(canvas);
                break;
            case TYPE_PIE:
                drawPieChart(canvas);
                break;
            case TYPE_BAR_VERTICAL:
                drawVerticalBarChart(canvas);
                break;
        }
    }

    /** 水平柱状图：用于热销TOP8 */
    private void drawBarChart(Canvas canvas) {
        int count = Math.min(data.size(), 8);
        float left = 160f;
        float right = getWidth() - 40f;
        float top = 80f;
        float bottom = getHeight() - 80f;
        float barHeight = (bottom - top) / count;
        float gap = 8f;

        // 找最大值
        double maxVal = 0;
        for (Object o : data) {
            TopProductWrapper w = wrapTop(o);
            if (w.value > maxVal) maxVal = w.value;
        }
        if (maxVal == 0) maxVal = 1;

        for (int i = 0; i < count; i++) {
            TopProductWrapper w = wrapTop(data.get(i));
            float y = top + i * barHeight;
            float barW = (float) (w.value / maxVal) * (right - left);
            float barTop = y + gap;
            float barBot = y + barHeight - gap;

            // 柱
            paintBar.setColor(w.color);
            canvas.drawRect(left, barTop, left + barW, barBot, paintBar);

            // 名次
            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setTextSize(30f);
            paintText.setColor(0xFF5D6D7E);
            canvas.drawText("#" + (i + 1), left - 12f, barBot - 4f, paintText);

            // 商品名
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setTextSize(30f);
            paintText.setColor(0xFF1B1F1B);
            String name = w.label.length() > 10 ? w.label.substring(0, 10) + "…" : w.label;
            canvas.drawText(name, left + 8f, barTop + 18f, paintText);

            // 数值
            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setTextSize(28f);
            paintText.setColor(0xFF5D6D7E);
            canvas.drawText(w.valueText, right - 8f, barBot - 4f, paintText);
        }
    }

    /** 垂直柱状图：用于24小时高峰分布 */
    private void drawVerticalBarChart(Canvas canvas) {
        float left = 80f;
        float right = getWidth() - 40f;
        float top = 80f;
        float bottom = getHeight() - 100f;
        float chartW = right - left;

        // 分组按 hour 排序
        java.util.Collections.sort((List<? extends Comparable>) data, (a, b) -> {
            int h1 = (data.get(0) instanceof PeakHour) ? ((PeakHour) a).hour : 0;
            int h2 = (data.get(0) instanceof PeakHour) ? ((PeakHour) b).hour : 0;
            return Integer.compare(h1, h2);
        });

        int count = data.size();
        float barW = chartW / count * 0.6f;
        float gap = chartW / count * 0.4f;

        double maxVal = 0;
        for (Object o : data) {
            double v = (o instanceof PeakHour) ? ((PeakHour) o).orderCount : 0;
            if (v > maxVal) maxVal = v;
        }
        if (maxVal == 0) maxVal = 1;

        for (int i = 0; i < count; i++) {
            PeakHour ph = (PeakHour) data.get(i);
            float x = left + i * (barW + gap) + gap / 2f;
            float barH = (float) (ph.orderCount / maxVal) * (bottom - top);
            float barTop = bottom - barH;

            paintBar.setColor(BAR_COLOR);
            canvas.drawRect(x, barTop, x + barW, bottom, paintBar);

            // 小时标签
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(24f);
            paintText.setColor(0xFF5D6D7E);
            canvas.drawText(ph.hour + ":00", x + barW / 2f, bottom + 40f, paintText);

            // 数值
            paintText.setTextSize(26f);
            paintText.setColor(0xFF1B1F1B);
            canvas.drawText(String.valueOf(ph.orderCount), x + barW / 2f, barTop - 12f, paintText);
        }
    }

    /** 折线图：用于近7天趋势 */
    private void drawLineChart(Canvas canvas) {
        float left = 100f;
        float right = getWidth() - 40f;
        float top = 100f;
        float bottom = getHeight() - 100f;

        // 网格线
        paintGrid.setColor(0xFFE8EAED);
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paintGrid);
        }

        // 按日期排序
        java.util.Collections.sort(data, (a, b) -> {
            String d1 = ((DailyTrend) a).date;
            String d2 = ((DailyTrend) b).date;
            return d1.compareTo(d2);
        });

        int count = data.size();
        if (count < 2) return;

        double maxVal = 0;
        for (Object o : data) {
            if (((DailyTrend) o).amount > maxVal) maxVal = ((DailyTrend) o).amount;
        }
        if (maxVal == 0) maxVal = 1;

        float stepX = (right - left) / (count - 1);

        // 画折线
        Path path = new Path();
        for (int i = 0; i < count; i++) {
            DailyTrend dt = (DailyTrend) data.get(i);
            float x = left + i * stepX;
            float y = (float) (bottom - (dt.amount / maxVal) * (bottom - top));
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, paintLine);

        // 画点 + 标签
        float dotRadius = 8f;
        paintBar.setColor(0xFFFFFFFF);
        for (int i = 0; i < count; i++) {
            DailyTrend dt = (DailyTrend) data.get(i);
            float x = left + i * stepX;
            float y = (float) (bottom - (dt.amount / maxVal) * (bottom - top));

            paintLine.setStrokeWidth(2f);
            canvas.drawCircle(x, y, dotRadius + 2f, paintBar);
            paintBar.setColor(LINE_COLOR);
            canvas.drawCircle(x, y, dotRadius, paintBar);
            paintBar.setColor(BAR_COLOR);

            // 日期
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(24f);
            paintText.setColor(0xFF5D6D7E);
            String label = dt.date.length() >= 5 ? dt.date.substring(5) : dt.date;
            canvas.drawText(label, x, bottom + 40f, paintText);

            // 金额
            paintText.setTextSize(26f);
            paintText.setColor(0xFF1B1F1B);
            canvas.drawText("¥" + String.format("%.0f", dt.amount), x, y - 24f, paintText);
        }
    }

    /** 饼图：用于客户画像 */
    private void drawPieChart(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 100f;

        double total = 0;
        for (Object o : data) {
            total += ((CustomerProfile) o).orderCount;
        }
        if (total == 0) total = 1;

        RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        float startAngle = -90f;
        int colorIdx = 0;

        for (int i = 0; i < data.size(); i++) {
            CustomerProfile cp = (CustomerProfile) data.get(i);
            float sweep = (float) (cp.orderCount / total * 360f);
            // 最小值保证可见
            if (sweep < 1f && data.size() > 1) sweep = 1f;

            paintPie.setColor(PIE_COLORS[colorIdx % PIE_COLORS.length]);
            canvas.drawArc(rect, startAngle, sweep, true, paintPie);

            // 标签
            float midAngle = startAngle + sweep / 2f;
            float labelRadius = radius * 0.65f;
            float lx = cx + (float) Math.cos(Math.toRadians(midAngle)) * labelRadius;
            float ly = cy + (float) Math.sin(Math.toRadians(midAngle)) * labelRadius;

            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(28f);
            paintText.setColor(0xFFFFFFFF);

            // 只显示占比 > 5% 的标签
            if (cp.orderCount / total > 0.05) {
                canvas.drawText(cp.buyerName, lx, ly - 10f, paintText);
                canvas.drawText(String.format("%.0f%%", cp.orderCount / total * 100), lx, ly + 24f, paintText);
            }

            startAngle += sweep;
            colorIdx++;
        }

        // 图例
        float legendX = 40f;
        float legendY = getHeight() - 60f;
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(26f);
        paintText.setColor(0xFF5D6D7E);
        StringBuilder legend = new StringBuilder("图例：");
        for (int i = 0; i < data.size(); i++) {
            CustomerProfile cp = (CustomerProfile) data.get(i);
            legend.append(cp.buyerName).append(" ")
                    .append(String.format("%.0f%%", cp.orderCount / total * 100))
                    .append("  ");
        }
        canvas.drawText(legend.toString(), legendX, legendY, paintText);
    }

    // Helper wrappers
    private static class TopProductWrapper {
        String label;
        double value;
        String valueText;
        int color;
    }

    private TopProductWrapper wrapTop(Object o) {
        TopProductWrapper w = new TopProductWrapper();
        if (o instanceof TopProduct) {
            TopProduct tp = (TopProduct) o;
            w.label = tp.productName;
            w.value = tp.salesCount;
            w.valueText = tp.salesCount + "件 · ¥" + String.format("%.0f", tp.salesAmount);
        } else {
            w.label = "—";
            w.value = 0;
            w.valueText = "0";
        }
        w.color = 0xFF2E7D32;
        return w;
    }
}
