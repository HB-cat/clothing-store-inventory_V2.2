package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.Models.SaleRecord;

import java.util.List;

public class SaleRecordAdapter extends RecyclerView.Adapter<SaleRecordAdapter.ViewHolder> {

    private List<SaleRecord> list;

    public SaleRecordAdapter(List<SaleRecord> list) {
        this.list = list;
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
        holder.tvInfo.setText(item.buyerName + " · " + item.saleTime + " · " + item.itemCount + "件");
        holder.tvAmount.setText("¥" + String.format("%.2f", item.totalAmount));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProduct, tvInfo, tvAmount;

        ViewHolder(View v) {
            super(v);
            tvProduct = v.findViewById(R.id.tv_record_product);
            tvInfo = v.findViewById(R.id.tv_record_info);
            tvAmount = v.findViewById(R.id.tv_record_amount);
        }
    }
}
