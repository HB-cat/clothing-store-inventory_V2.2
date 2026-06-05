package com.heben.client.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.adapter.AppointmentAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Appointment;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private AppointmentAdapter adapter;
    private List<Appointment> list = new ArrayList<>();
    private TextView tabPending, tabConfirmed;
    private boolean showPending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tabPending = findViewById(R.id.tab_pending);
        tabConfirmed = findViewById(R.id.tab_confirmed);
        rv = findViewById(R.id.rv_appointments);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppointmentAdapter(list, new AppointmentAdapter.OnAction() {
            @Override
            public void onConfirm(int pos) {
                Appointment a = list.get(pos);
                ApiClient.getService().confirmAppointment(a.id).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                            Toast.makeText(AppointmentsActivity.this, "已确认", Toast.LENGTH_SHORT).show();
                            load();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }

            @Override
            public void onReject(int pos) {
                Appointment a = list.get(pos);
                HashMap<String, String> body = new HashMap<>();
                body.put("reason", "商家拒绝");
                ApiClient.getService().rejectAppointment(a.id, body).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                            Toast.makeText(AppointmentsActivity.this, "已拒绝", Toast.LENGTH_SHORT).show();
                            load();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }
        });
        rv.setAdapter(adapter);

        tabPending.setOnClickListener(v -> switchTab(true));
        tabConfirmed.setOnClickListener(v -> switchTab(false));

        switchTab(true);
    }

    private void switchTab(boolean pending) {
        showPending = pending;
        tabPending.setSelected(pending);
        tabConfirmed.setSelected(!pending);
        load();
    }

    private void load() {
        String status = showPending ? "pending" : "confirmed";
        ApiClient.getService().getAppointments(status).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    list.clear();
                    List<Appointment> data = response.body().getData();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {}
        });
    }
}
