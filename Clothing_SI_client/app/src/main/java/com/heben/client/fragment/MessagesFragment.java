package com.heben.client.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.heben.client.R;
import com.heben.client.activity.ChatDetailActivity;
import com.heben.client.adapter.AppointmentAdapter;
import com.heben.client.adapter.ChatSessionAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Appointment;
import com.heben.client.model.Models.ChatSession;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesFragment extends Fragment {

    private View tabChat, tabAppt, chatContent, apptContent;
    private RecyclerView rvSessions;
    private RecyclerView rvApptsPending, rvApptsConfirmed;
    private ChatSessionAdapter sessionAdapter;
    private AppointmentAdapter pendingAdapter, confirmedAdapter;
    private List<ChatSession> sessions = new ArrayList<>();
    private List<Appointment> pendingAppts = new ArrayList<>();
    private List<Appointment> confirmedAppts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_messages, container, false);

        swipeRefresh = v.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> {
            loadSessions();
            loadAppointments();
            swipeRefresh.postDelayed(() -> {
                if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            }, 2000);
        });

        tabChat = v.findViewById(R.id.tab_chat);
        tabAppt = v.findViewById(R.id.tab_appt);
        chatContent = v.findViewById(R.id.chat_content);
        apptContent = v.findViewById(R.id.appt_content);

        rvSessions = v.findViewById(R.id.rv_chat_sessions);
        rvSessions.setLayoutManager(new LinearLayoutManager(getActivity()));
        sessionAdapter = new ChatSessionAdapter(sessions, session -> {
            Intent intent = new Intent(getActivity(), ChatDetailActivity.class);
            intent.putExtra("sessionId", session.id);
            intent.putExtra("otherName", session.otherName);
            startActivity(intent);
        });
        rvSessions.setAdapter(sessionAdapter);

        // Pending appointments
        rvApptsPending = v.findViewById(R.id.rv_appointments_pending);
        rvApptsPending.setLayoutManager(new LinearLayoutManager(getActivity()));
        pendingAdapter = new AppointmentAdapter(pendingAppts, new AppointmentAdapter.OnAction() {
            @Override
            public void onConfirm(int pos) {
                Appointment a = pendingAppts.get(pos);
                ApiClient.getService().confirmAppointment(a.id).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) { loadAppointments(); }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }

            @Override
            public void onReject(int pos) {
                Appointment a = pendingAppts.get(pos);
                HashMap<String, String> body = new HashMap<>();
                body.put("reason", "商家拒绝");
                ApiClient.getService().rejectAppointment(a.id, body).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) { loadAppointments(); }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }
        }, true);
        rvApptsPending.setAdapter(pendingAdapter);

        // Confirmed appointments
        rvApptsConfirmed = v.findViewById(R.id.rv_appointments_confirmed);
        rvApptsConfirmed.setLayoutManager(new LinearLayoutManager(getActivity()));
        confirmedAdapter = new AppointmentAdapter(confirmedAppts, null, false, "已确认");
        rvApptsConfirmed.setAdapter(confirmedAdapter);

        tabChat.setOnClickListener(x -> switchTab("chat"));
        tabAppt.setOnClickListener(x -> switchTab("appt"));
        tabChat.setSelected(true);

        switchTab("chat");
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSessions();
        loadAppointments();
    }

    private void switchTab(String tab) {
        boolean isChat = tab.equals("chat");
        tabChat.setSelected(isChat);
        tabAppt.setSelected(!isChat);
        chatContent.setVisibility(isChat ? View.VISIBLE : View.GONE);
        apptContent.setVisibility(isChat ? View.GONE : View.VISIBLE);
        if (!isChat) loadAppointments();
    }

    private void loadSessions() {
        ApiClient.getService().getChatSessions().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatSession>>> call, Response<ApiResponse<List<ChatSession>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    sessions.clear();
                    List<ChatSession> data = response.body().getData();
                    if (data != null) sessions.addAll(data);
                    sessionAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<ChatSession>>> call, Throwable t) {}
        });
    }

    private void loadAppointments() {
        // Pending
        ApiClient.getService().getAppointments("pending").enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    pendingAppts.clear();
                    List<Appointment> data = response.body().getData();
                    if (data != null) pendingAppts.addAll(data);
                    pendingAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {}
        });

        // Confirmed
        ApiClient.getService().getAppointments("confirmed").enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    confirmedAppts.clear();
                    List<Appointment> data = response.body().getData();
                    if (data != null) confirmedAppts.addAll(data);
                    confirmedAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {}
        });
    }
}
