package com.heben.client.agent;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.heben.client.R;
import com.heben.client.model.Models.AgentSummaryResponse;
import com.heben.client.model.Models.HotProduct;
import com.heben.client.model.Models.StockWarning;

/**
 * 智能体富文本卡片构建器
 * 将 AgentSummaryResponse 渲染为 view_agent_card.xml 布局
 * 带操作按钮：去补货 → PurchaseActivity，详细分析 → AnalyticsActivity
 */
public class AgentRichCardHelper {

    /**
     * 构建经营简报卡片 View
     */
    public static View buildSummaryCard(Context context, AgentSummaryResponse data,
                                        Runnable onRestock, Runnable onDetail) {
        View card = LayoutInflater.from(context).inflate(R.layout.view_agent_card, null);

        // ===== Header =====
        View header = card.findViewById(R.id.card_header);
        TextView headerTitle = card.findViewById(R.id.card_header_title);
        TextView headerIcon = card.findViewById(R.id.card_header_icon);
        header.setVisibility(View.VISIBLE);
        headerIcon.setText("📊");
        headerTitle.setText("今日经营简报");

        // ===== 数据区 =====
        LinearLayout dataSection = card.findViewById(R.id.card_data_section);
        TextView tvAmount = card.findViewById(R.id.tv_amount);
        TextView tvProfit = card.findViewById(R.id.tv_profit);
        TextView tvOrders = card.findViewById(R.id.tv_orders);
        TextView tvSummary = card.findViewById(R.id.tv_summary);

        if (data.todayBrief != null) {
            dataSection.setVisibility(View.VISIBLE);
            tvAmount.setText("¥" + String.format("%.2f", data.todayBrief.totalAmount));
            tvProfit.setText("¥" + String.format("%.2f", data.todayBrief.totalProfit));
            tvOrders.setText(data.todayBrief.orderCount + " 单");
            if (data.todayBrief.summary != null && !data.todayBrief.summary.isEmpty()) {
                tvSummary.setVisibility(View.VISIBLE);
                tvSummary.setText("📝 " + data.todayBrief.summary);
            }
        }

        // ===== 库存预警区 =====
        LinearLayout warningSection = card.findViewById(R.id.card_warning_section);
        LinearLayout warningList = card.findViewById(R.id.warning_list);

        if (data.stockWarnings != null && !data.stockWarnings.isEmpty()) {
            card.findViewById(R.id.divider_1).setVisibility(View.VISIBLE);
            warningSection.setVisibility(View.VISIBLE);
            for (StockWarning w : data.stockWarnings) {
                warningList.addView(buildWarningItem(context, w));
            }
        }

        // ===== 热销商品区 =====
        LinearLayout hotSection = card.findViewById(R.id.card_hot_section);
        LinearLayout hotList = card.findViewById(R.id.hot_list);

        if (data.hotProducts != null && !data.hotProducts.isEmpty()) {
            card.findViewById(R.id.divider_2).setVisibility(View.VISIBLE);
            hotSection.setVisibility(View.VISIBLE);
            for (int i = 0; i < data.hotProducts.size(); i++) {
                hotList.addView(buildHotItem(context, i + 1, data.hotProducts.get(i)));
            }
        }

        // ===== 建议 =====
        TextView tvSuggestion = card.findViewById(R.id.tv_suggestion);
        if (data.suggestion != null && !data.suggestion.isEmpty()) {
            card.findViewById(R.id.divider_3).setVisibility(View.VISIBLE);
            tvSuggestion.setVisibility(View.VISIBLE);
            tvSuggestion.setText("💡 " + data.suggestion);
        }

        // ===== 操作按钮 =====
        LinearLayout actions = card.findViewById(R.id.card_actions);
        boolean hasWarnings = data.stockWarnings != null && !data.stockWarnings.isEmpty();
        boolean hasData = data.todayBrief != null;

        if (hasWarnings || hasData) {
            actions.setVisibility(View.VISIBLE);
            TextView btnRestock = card.findViewById(R.id.btn_restock);
            TextView btnDetail = card.findViewById(R.id.btn_detail);

            // 无预警时隐藏补货按钮
            if (!hasWarnings) {
                btnRestock.setVisibility(View.GONE);
            } else {
                btnRestock.setOnClickListener(v -> {
                    if (onRestock != null) onRestock.run();
                });
            }

            btnDetail.setOnClickListener(v -> {
                if (onDetail != null) onDetail.run();
            });
        }

        return card;
    }

    /** 构建预警商品行 */
    private static View buildWarningItem(Context context, StockWarning w) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_agent_card_row, null);
        TextView tvIcon = row.findViewById(R.id.row_icon);
        TextView tvText = row.findViewById(R.id.row_text);

        tvIcon.setText(w.stock <= 2 ? "🔴" : "🟡");
        String attrs = (w.attrs != null && !w.attrs.isEmpty()) ? "(" + w.attrs + ")" : "";
        tvText.setText(w.productName + attrs + "  仅剩 " + w.stock + " 件");
        return row;
    }

    /** 构建热销商品行 */
    private static View buildHotItem(Context context, int rank, HotProduct h) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_agent_card_row, null);
        TextView tvIcon = row.findViewById(R.id.row_icon);
        TextView tvText = row.findViewById(R.id.row_text);

        tvIcon.setText(rank + ".");
        tvText.setText(h.productName
                + "  ×" + h.salesCount
                + "  ¥" + String.format("%.1f", h.salesAmount));
        return row;
    }
}
