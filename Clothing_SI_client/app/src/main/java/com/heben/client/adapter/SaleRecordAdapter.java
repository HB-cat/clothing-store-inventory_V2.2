package com.heben.client.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.model.Models.SaleRecordItem;
import com.heben.client.network.ApiClient;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SaleRecordAdapter extends RecyclerView.Adapter<SaleRecordAdapter.ViewHolder> {

    private static final String[] EMOJIS = {"👕", "👗", "👔", "🧥", "👖", "👟", "🧢", "👒", "🧣", "🎒"};
    private List<SaleRecord> list;
    private Runnable onRefresh;

    public SaleRecordAdapter(List<SaleRecord> list, Runnable onRefresh) {
        this.list = list;
        this.onRefresh = onRefresh;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SaleRecord item = list.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        // Emoji thumb
        holder.tvThumb.setText(EMOJIS[position % EMOJIS.length]);

        // Product names
        String products = "";
        if (item.items != null && !item.items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < item.items.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(item.items.get(i).productName);
            }
            products = sb.toString();
        }
        holder.tvProduct.setText(products.isEmpty() ? "销售#" + item.id : products);

        // Info: buyer · time · count
        holder.tvInfo.setText(item.buyerName + " · " + item.saleTime + " · " + item.itemCount + "件");

        // Amount
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.CHINA);
        nf.setMaximumFractionDigits(0);
        holder.tvAmount.setText("¥" + nf.format(item.totalAmount));

        // Compute return status per item
        boolean allReturned = true;
        boolean anyReturnable = false;
        if (item.items != null) {
            for (SaleRecordItem i : item.items) {
                if (i.returned == 0) { allReturned = false; anyReturnable = true; }
            }
        }

        if (allReturned || item.items == null || item.items.isEmpty()) {
            holder.tvReturn.setText("已退");
            holder.tvReturn.setTextColor(0xFF27AE60);
            holder.tvReturn.setClickable(false);
        } else {
            holder.tvReturn.setText("退货");
            holder.tvReturn.setTextColor(0xFFE74C3C);
            holder.tvReturn.setClickable(true);
            holder.tvReturn.setOnClickListener(v -> showReturnDialog(ctx, item));
        }
    }

    private void showReturnDialog(android.content.Context ctx, SaleRecord record) {
        if (record.items == null || record.items.isEmpty()) return;

        // Build dialog content: each item as a row with its own return button
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.CHINA);
        nf.setMaximumFractionDigits(0);

        for (int i = 0; i < record.items.size(); i++) {
            SaleRecordItem si = record.items.get(i);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);
            row.setWeightSum(1);

            // Item info
            TextView tvInfo = new TextView(ctx);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f));
            tvInfo.setTextSize(14);
            String emoji = EMOJIS[i % EMOJIS.length];
            tvInfo.setText(emoji + " " + si.productName + "  ¥" + nf.format(si.unitPrice));
            row.addView(tvInfo);

            // Return button or status
            TextView tvAction = new TextView(ctx);
            tvAction.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f));
            tvAction.setGravity(android.view.Gravity.END);
            tvAction.setTextSize(13);

            if (si.returned == 1) {
                tvAction.setText("已退");
                tvAction.setTextColor(0xFF27AE60);
            } else {
                tvAction.setText("退货");
                tvAction.setTextColor(0xFFE74C3C);
                tvAction.setClickable(true);
                tvAction.setFocusable(true);
                Long saleItemId = si.id;
                int index = i;
                tvAction.setOnClickListener(v -> {
                    callReturnApi(ctx, saleItemId, () -> {
                        si.returned = 1;
                        tvAction.setText("已退");
                        tvAction.setTextColor(0xFF27AE60);
                        tvAction.setClickable(false);
                        notifyDataSetChanged();
                    });
                });
            }
            row.addView(tvAction);
            layout.addView(row);
        }

        new AlertDialog.Builder(ctx)
                .setTitle("选择退货商品")
                .setView(layout)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void callReturnApi(android.content.Context ctx, Long saleItemId, Runnable onSuccess) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("saleItemId", saleItemId);

        ApiClient.getService().returnSale(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ctx, "退货成功", Toast.LENGTH_SHORT).show();
                    if (onSuccess != null) onSuccess.run();
                    if (onRefresh != null) onRefresh.run();
                } else {
                    Toast.makeText(ctx, "退货失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(ctx, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvThumb, tvProduct, tvInfo, tvAmount, tvReturn;

        ViewHolder(View v) {
            super(v);
            tvThumb = v.findViewById(R.id.tv_record_thumb);
            tvProduct = v.findViewById(R.id.tv_record_product);
            tvInfo = v.findViewById(R.id.tv_record_info);
            tvAmount = v.findViewById(R.id.tv_record_amount);
            tvReturn = v.findViewById(R.id.tv_record_return);
        }
    }
}
