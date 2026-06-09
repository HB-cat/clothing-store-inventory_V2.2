package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.Models.DraftBill;

import java.util.List;

public class DraftBillAdapter extends RecyclerView.Adapter<DraftBillAdapter.ViewHolder> {

    private List<DraftBill> list;
    public interface OnDraftAction { void onEdit(int position); void onDelete(int position); void onSubmit(int position); }
    private OnDraftAction listener;

    public DraftBillAdapter(List<DraftBill> list, OnDraftAction listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draft_bill, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DraftBill d = list.get(position);
        holder.tvNo.setText("草稿 #" + d.billIndex);
        holder.tvInfo.setText(d.itemCount + "件商品 · ¥" + String.format("%.2f", d.totalAmount));
        int pos = position;
        holder.itemView.setOnClickListener(v -> listener.onEdit(pos));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(pos));
        holder.btnSubmit.setOnClickListener(v -> listener.onSubmit(pos));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNo, tvInfo;
        View btnDelete;
        MaterialButton btnSubmit;
        ViewHolder(View v) {
            super(v);
            tvNo = v.findViewById(R.id.tv_draft_no);
            tvInfo = v.findViewById(R.id.tv_draft_info);
            btnDelete = v.findViewById(R.id.btn_delete_draft);
            btnSubmit = v.findViewById(R.id.btn_submit_draft);
        }
    }
}
