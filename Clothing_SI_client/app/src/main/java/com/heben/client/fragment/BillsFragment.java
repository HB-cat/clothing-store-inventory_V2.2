package com.heben.client.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.activity.SaleActivity;
import com.heben.client.adapter.SaleRecordAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SalesSummary;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillsFragment extends Fragment {

    private TextView tvAmount, tvProfit, tvCount;
    private CardView cardDraft;
    private TextView tvDraftTitle, tvDraftSubtitle;
    private RecyclerView rvRecords;
    private SaleRecordAdapter adapter;
    private List<SaleRecord> records = new ArrayList<>();
    private String currentPeriod = "today";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bills, container, false);

        tvAmount = v.findViewById(R.id.tv_sales_amount);
        tvProfit = v.findViewById(R.id.tv_sales_profit);
        tvCount = v.findViewById(R.id.tv_sales_count);
        cardDraft = v.findViewById(R.id.card_draft);
        tvDraftTitle = v.findViewById(R.id.tv_draft_title);
        tvDraftSubtitle = v.findViewById(R.id.tv_draft_subtitle);

        rvRecords = v.findViewById(R.id.rv_sales_records);
        rvRecords.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new SaleRecordAdapter(records);
        rvRecords.setAdapter(adapter);

        v.findViewById(R.id.btn_today).setOnClickListener(x -> switchPeriod("today"));
        v.findViewById(R.id.btn_month).setOnClickListener(x -> switchPeriod("month"));
        v.findViewById(R.id.btn_year).setOnClickListener(x -> switchPeriod("year"));
        v.findViewById(R.id.fab_add_sale).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), SaleActivity.class)));

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void switchPeriod(String period) {
        currentPeriod = period;
        loadData();
    }

    private void loadData() {
        loadSummary();
        loadRecords();
        loadDrafts();
    }

    private void loadSummary() {
        ApiService api = ApiClient.getService();
        Call<ApiResponse<SalesSummary>> call;
        switch (currentPeriod) {
            case "month": call = api.getMonthSales(); break;
            case "year":  call = api.getYearSales(); break;
            default:      call = api.getTodaySales(); break;
        }
        call.enqueue(new Callback<ApiResponse<SalesSummary>>() {
            @Override
            public void onResponse(Call<ApiResponse<SalesSummary>> call, Response<ApiResponse<SalesSummary>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SalesSummary data = response.body().getData();
                    if (data != null) {
                        tvAmount.setText("¥" + String.format("%.2f", data.totalAmount));
                        tvProfit.setText("¥" + String.format("%.2f", data.totalProfit));
                        tvCount.setText(String.valueOf(data.totalCount));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SalesSummary>> call, Throwable t) {
                // silent fail — stats show 0 by default
            }
        });
    }

    private void loadRecords() {
        ApiService api = ApiClient.getService();
        api.getSalesRecords(null, null).enqueue(new Callback<ApiResponse<List<SaleRecord>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SaleRecord>>> call, Response<ApiResponse<List<SaleRecord>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SaleRecord> data = response.body().getData();
                    records.clear();
                    if (data != null) records.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SaleRecord>>> call, Throwable t) {
                // silent fail
            }
        });
    }

    private void loadDrafts() {
        ApiService api = ApiClient.getService();
        api.getDrafts().enqueue(new Callback<ApiResponse<List<com.heben.client.model.Models.DraftBill>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.heben.client.model.Models.DraftBill>>> call, Response<ApiResponse<List<com.heben.client.model.Models.DraftBill>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.heben.client.model.Models.DraftBill> drafts = response.body().getData();
                    if (drafts != null && !drafts.isEmpty()) {
                        com.heben.client.model.Models.DraftBill d = drafts.get(0);
                        tvDraftTitle.setText("📄 草稿账单 #" + d.draftNo);
                        tvDraftSubtitle.setText(d.itemCount + "件商品 · ¥" + String.format("%.2f", d.totalAmount));
                        cardDraft.setVisibility(View.VISIBLE);
                    } else {
                        cardDraft.setVisibility(View.GONE);
                    }
                } else {
                    cardDraft.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.heben.client.model.Models.DraftBill>>> call, Throwable t) {
                cardDraft.setVisibility(View.GONE);
            }
        });
    }
}
