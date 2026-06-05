package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.Models.SharedProduct;

import java.util.List;

public class SharedProductAdapter extends RecyclerView.Adapter<SharedProductAdapter.ViewHolder> {

    private List<SharedProduct> list;
    public interface OnWholesaleClick { void onClick(int position); }
    private OnWholesaleClick listener;

    public SharedProductAdapter(List<SharedProduct> list, OnWholesaleClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shared_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SharedProduct p = list.get(position);
        holder.tvName.setText(p.productName);
        holder.tvWholesaler.setText("商家: " + p.wholesalerName);
        holder.tvPrice.setText("¥" + String.format("%.2f", p.sharedPrice));
        int pos = position;
        holder.btnWholesale.setOnClickListener(v -> listener.onClick(pos));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvWholesaler, tvPrice;
        MaterialButton btnWholesale;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_product_name);
            tvWholesaler = v.findViewById(R.id.tv_product_wholesaler);
            tvPrice = v.findViewById(R.id.tv_product_price);
            btnWholesale = v.findViewById(R.id.btn_wholesale);
        }
    }
}
