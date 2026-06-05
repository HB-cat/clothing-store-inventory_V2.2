package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.Models.Product;

import java.util.List;

public class PurchaseProductAdapter extends RecyclerView.Adapter<PurchaseProductAdapter.ViewHolder> {

    public interface OnProductSelected {
        void onSelected(Product product);
    }

    private List<Product> list;
    private OnProductSelected listener;
    private int selectedPos = -1;

    public PurchaseProductAdapter(List<Product> list, OnProductSelected listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product item = list.get(position);
        holder.tvName.setText(item.name);
        holder.tvPrice.setText("进价 ¥" + String.format("%.2f", item.purchasePrice));
        holder.itemView.setSelected(position == selectedPos);
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPos;
            selectedPos = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPos);
            listener.onSelected(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_product_name);
            tvPrice = v.findViewById(R.id.tv_product_price);
        }
    }
}
