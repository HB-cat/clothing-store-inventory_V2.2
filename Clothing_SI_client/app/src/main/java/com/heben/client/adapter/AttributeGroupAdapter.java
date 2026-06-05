package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.Models.AttributeGroup;
import com.heben.client.model.Models.AttributeValue;

import java.util.List;

public class AttributeGroupAdapter extends RecyclerView.Adapter<AttributeGroupAdapter.ViewHolder> {

    private List<AttributeGroup> list;
    public interface OnGroupAction { void onDeleteGroup(int position); void onAddValue(int position); void onDeleteValue(int groupPos, int valuePos); }
    private OnGroupAction listener;

    public AttributeGroupAdapter(List<AttributeGroup> list, OnGroupAction listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attribute_group, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttributeGroup g = list.get(position);
        holder.tvName.setText(g.name);
        int pos = position;
        holder.btnDeleteGroup.setOnClickListener(v -> listener.onDeleteGroup(pos));
        holder.btnAddValue.setOnClickListener(v -> listener.onAddValue(pos));

        holder.layoutValues.removeAllViews();
        if (g.values != null) {
            for (int i = 0; i < g.values.size(); i++) {
                AttributeValue av = g.values.get(i);
                View row = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_draft_item, holder.layoutValues, false);
                ((TextView) row.findViewById(R.id.tv_draft_item_name)).setText(av.value);
                row.findViewById(R.id.tv_draft_item_qty).setVisibility(View.GONE);
                row.findViewById(R.id.tv_draft_item_price).setVisibility(View.GONE);
                TextView deleteBtn = new TextView(holder.itemView.getContext());
                deleteBtn.setText("✕");
                deleteBtn.setTextSize(12);
                deleteBtn.setTextColor(0xFFE85D3A);
                deleteBtn.setPadding(8, 8, 8, 8);
                deleteBtn.setClickable(true);
                deleteBtn.setFocusable(true);
                int finalI = i;
                deleteBtn.setOnClickListener(v -> listener.onDeleteValue(pos, finalI));
                ((LinearLayout) row).addView(deleteBtn);
                holder.layoutValues.addView(row);
            }
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        View btnDeleteGroup;
        MaterialButton btnAddValue;
        LinearLayout layoutValues;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_group_name);
            btnDeleteGroup = v.findViewById(R.id.btn_delete_group);
            btnAddValue = v.findViewById(R.id.btn_add_value);
            layoutValues = v.findViewById(R.id.layout_values);
        }
    }
}
