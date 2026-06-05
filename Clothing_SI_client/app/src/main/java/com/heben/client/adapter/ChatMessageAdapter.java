package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.Models.ChatMessage;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private List<ChatMessage> list;

    public ChatMessageAdapter(List<ChatMessage> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage m = list.get(position);
        if (m.isMine) {
            holder.layoutOther.setVisibility(View.GONE);
            holder.layoutMine.setVisibility(View.VISIBLE);
            holder.tvMineContent.setText(m.content);
        } else {
            holder.layoutMine.setVisibility(View.GONE);
            holder.layoutOther.setVisibility(View.VISIBLE);
            holder.tvOtherContent.setText(m.content);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutMine, layoutOther;
        TextView tvMineContent, tvOtherContent;

        ViewHolder(View v) {
            super(v);
            layoutMine = v.findViewById(R.id.layout_message_mine);
            layoutOther = v.findViewById(R.id.layout_message_other);
            tvMineContent = v.findViewById(R.id.tv_mine_content);
            tvOtherContent = v.findViewById(R.id.tv_other_content);
        }
    }
}
