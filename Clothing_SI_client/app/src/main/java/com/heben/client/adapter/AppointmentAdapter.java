package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.Models.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> list;
    public interface OnAction { void onConfirm(int position); void onReject(int position); }
    private OnAction listener;

    public AppointmentAdapter(List<Appointment> list, OnAction listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment a = list.get(position);
        holder.tvTitle.setText(a.buyerName + " · " + a.timeSlot);
        holder.tvSubtitle.setText(a.productName);
        int pos = position;
        holder.btnConfirm.setOnClickListener(v -> listener.onConfirm(pos));
        holder.btnReject.setOnClickListener(v -> listener.onReject(pos));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        MaterialButton btnConfirm, btnReject;
        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_appt_title);
            tvSubtitle = v.findViewById(R.id.tv_appt_subtitle);
            btnConfirm = v.findViewById(R.id.btn_confirm_appt);
            btnReject = v.findViewById(R.id.btn_reject_appt);
        }
    }
}
