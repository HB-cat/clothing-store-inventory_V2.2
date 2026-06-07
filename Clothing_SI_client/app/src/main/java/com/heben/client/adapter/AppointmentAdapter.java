package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.Models.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    public interface OnAction {
        void onConfirm(int position);
        void onReject(int position);
    }

    private List<Appointment> list;
    private OnAction listener;
    private boolean showActions;
    private String statusBadge = "";

    public AppointmentAdapter(List<Appointment> list, OnAction listener, boolean showActions) {
        this.list = list;
        this.listener = listener;
        this.showActions = showActions;
    }

    public AppointmentAdapter(List<Appointment> list, OnAction listener, boolean showActions, String statusBadge) {
        this.list = list;
        this.listener = listener;
        this.showActions = showActions;
        this.statusBadge = statusBadge;
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

        // Time slot - show timeRange or appointmentDate
        String timeDisplay = (a.timeSlot != null && !a.timeSlot.isEmpty())
                ? a.timeSlot.replace("-", "\n")
                : (a.appointmentDate != null ? a.appointmentDate.substring(0, 5) : "");
        holder.tvTime.setText(timeDisplay);

        // Name
        holder.tvName.setText(a.buyerName != null ? a.buyerName : "客户");

        // Info
        String info = "";
        if (a.productName != null && !a.productName.isEmpty()) {
            if (a.status != null && a.status.equals("confirmed")) {
                info = "已确认 · " + a.productName;
            } else {
                info = a.productName;
            }
        }
        if (a.attrsDisplay != null && !a.attrsDisplay.isEmpty()) {
            info += " · " + a.attrsDisplay;
        }
        holder.tvInfo.setText(info);

        // Actions or status
        if (showActions) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.tvStatus.setVisibility(View.GONE);
            int pos = position;
            holder.btnConfirm.setOnClickListener(v -> {
                if (listener != null) listener.onConfirm(pos);
            });
            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(pos);
            });
        } else {
            holder.layoutActions.setVisibility(View.GONE);
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(statusBadge.isEmpty() ? "已确认" : statusBadge);
            holder.tvStatus.setBackgroundColor(0xFFE8F8F5);
            holder.tvStatus.setTextColor(0xFF1ABC9C);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvName, tvInfo, tvStatus;
        View btnConfirm, btnReject;
        LinearLayout layoutActions;

        ViewHolder(View v) {
            super(v);
            tvTime = v.findViewById(R.id.tv_appt_time);
            tvName = v.findViewById(R.id.tv_appt_name);
            tvInfo = v.findViewById(R.id.tv_appt_info);
            tvStatus = v.findViewById(R.id.tv_appt_status);
            btnConfirm = v.findViewById(R.id.btn_confirm_appt);
            btnReject = v.findViewById(R.id.btn_reject_appt);
            layoutActions = v.findViewById(R.id.layout_appt_actions);
        }
    }
}
