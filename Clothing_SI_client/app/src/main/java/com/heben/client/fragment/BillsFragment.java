package com.heben.client.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.activity.NewOrderActivity;
import com.heben.client.activity.SaleActivity;
import com.heben.client.adapter.SaleRecordAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SalesSummary;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillsFragment extends Fragment {

    private TextView tvAmount, tvProfit, tvCount;
    private View cardDraft;
    private TextView tvDraftTitle, tvDraftSubtitle;
    private RecyclerView rvRecords;
    private SaleRecordAdapter adapter;
    private List<SaleRecord> records = new ArrayList<>();

    // Date selector
    private TextView tvDateLabel;
    private TextView chipToday, chipMonth, chipYear;

    private LocalDate selectedDate;
    private String activePreset = "today"; // "today" | "month" | "year" | "custom"

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

        // Date selector
        tvDateLabel = v.findViewById(R.id.tv_date_label);
        chipToday = v.findViewById(R.id.chip_today);
        chipMonth = v.findViewById(R.id.chip_month);
        chipYear = v.findViewById(R.id.chip_year);

        rvRecords = v.findViewById(R.id.rv_sales_records);
        rvRecords.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new SaleRecordAdapter(records, () -> loadData());
        rvRecords.setAdapter(adapter);
        rvRecords.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        // Init
        selectedDate = LocalDate.now();
        applyPreset("today");

        // Click date bar → DatePickerDialog
        v.findViewById(R.id.date_selector).setOnClickListener(v1 -> {
            int y = selectedDate.getYear();
            int m = selectedDate.getMonthValue() - 1;
            int d = selectedDate.getDayOfMonth();
            new DatePickerDialog(getActivity(), (view, year, month, dayOfMonth) -> {
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                datePicked();
            }, y, m, d).show();
        });

        // Quick presets
        chipToday.setOnClickListener(v1 -> applyPreset("today"));
        chipMonth.setOnClickListener(v1 -> applyPreset("month"));
        chipYear.setOnClickListener(v1 -> applyPreset("year"));

        // FAB
        v.findViewById(R.id.fab_add_sale).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), SaleActivity.class)));

        // Draft card
        cardDraft.setOnClickListener(x ->
                startActivity(new Intent(getActivity(), NewOrderActivity.class)));

        return v;
    }

    private void datePicked() {
        activePreset = "custom";
        updateDateDisplay();
        // Deselect all presets
        chipToday.setTextColor(0xFF95A5A6);
        chipMonth.setTextColor(0xFF95A5A6);
        chipYear.setTextColor(0xFF95A5A6);
        loadData();
    }

    private void applyPreset(String preset) {
        activePreset = preset;
        LocalDate now = LocalDate.now();
        switch (preset) {
            case "today":
                selectedDate = now;
                chipToday.setTextColor(0xFFE85D3A);
                chipMonth.setTextColor(0xFF95A5A6);
                chipYear.setTextColor(0xFF95A5A6);
                break;
            case "month":
                selectedDate = now.withDayOfMonth(1);
                chipToday.setTextColor(0xFF95A5A6);
                chipMonth.setTextColor(0xFFE85D3A);
                chipYear.setTextColor(0xFF95A5A6);
                break;
            case "year":
                selectedDate = now.withDayOfYear(1);
                chipToday.setTextColor(0xFF95A5A6);
                chipMonth.setTextColor(0xFF95A5A6);
                chipYear.setTextColor(0xFFE85D3A);
                break;
        }
        updateDateDisplay();
        loadData();
    }

    private void updateDateDisplay() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        tvDateLabel.setText("📅 " + selectedDate.format(fmt));
    }

    @Override
    public void onResume() {
        super.onResume();
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
        switch (activePreset) {
            case "month":
                call = api.getMonthSales();
                break;
            case "year":
                call = api.getYearSales();
                break;
            case "custom": {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                call = api.getSalesSummary(selectedDate.format(fmt), selectedDate.format(fmt));
                break;
            }
            default:
                call = api.getTodaySales();
                break;
        }
        call.enqueue(new Callback<ApiResponse<SalesSummary>>() {
            @Override
            public void onResponse(Call<ApiResponse<SalesSummary>> call, Response<ApiResponse<SalesSummary>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SalesSummary data = response.body().getData();
                    if (data != null) {
                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.CHINA);
                        nf.setMaximumFractionDigits(0);
                        tvAmount.setText("¥" + nf.format((long) data.totalAmount));
                        tvProfit.setText("¥" + nf.format((long) data.totalProfit));
                        tvCount.setText(String.valueOf(data.totalCount));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SalesSummary>> call, Throwable t) {}
        });
    }

    private void loadRecords() {
        ApiService api = ApiClient.getService();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr = selectedDate.format(fmt);
        api.getSalesRecords(dateStr, dateStr)
                .enqueue(new Callback<ApiResponse<List<SaleRecord>>>() {
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
            public void onFailure(Call<ApiResponse<List<SaleRecord>>> call, Throwable t) {}
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
