package com.heben.client.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.model.Models.SaleRecordItem;
import com.heben.client.network.ApiClient;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerOrdersActivity extends AppCompatActivity {

    private static final String[] EMOJIS = {"👕", "👗", "👔", "🧥", "👖", "👟", "🧢", "👒", "🧣", "🎒"};

    private RecyclerView rv;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private List<SaleRecord> allOrders = new ArrayList<>();
    private List<SaleRecord> filtered = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_orders);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_orders);
        tvEmpty = findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(filtered);
        rv.setAdapter(adapter);

        // Search
        android.widget.EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
        });

        loadOrders();
    }

    private void loadOrders() {
        ApiClient.getService().getBuyerOrders().enqueue(new Callback<ApiResponse<List<SaleRecord>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SaleRecord>>> call,
                                   Response<ApiResponse<List<SaleRecord>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SaleRecord> data = response.body().getData();
                    if (data != null) {
                        allOrders.clear();
                        allOrders.addAll(data);
                        filter(((android.widget.EditText)findViewById(R.id.et_search)).getText().toString());
                        return;
                    }
                }
                tvEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SaleRecord>>> call, Throwable t) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("网络错误，请稍后重试");
                rv.setVisibility(View.GONE);
            }
        });
    }

    private void filter(String keyword) {
        filtered.clear();
        if (keyword.trim().isEmpty()) {
            filtered.addAll(allOrders);
        } else {
            String kw = keyword.toLowerCase();
            for (SaleRecord r : allOrders) {
                if (r.items != null) {
                    for (SaleRecordItem i : r.items) {
                        if (i.productName != null && i.productName.toLowerCase().contains(kw)) {
                            filtered.add(r);
                            break;
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ===== Adapter =====
    private static class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {
        private final List<SaleRecord> list;

        OrderAdapter(List<SaleRecord> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_buyer_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            SaleRecord r = list.get(pos);
            h.tvThumb.setText(EMOJIS[pos % EMOJIS.length]);

            // Product names
            StringBuilder sb = new StringBuilder();
            if (r.items != null) {
                for (int i = 0; i < r.items.size(); i++) {
                    if (i > 0) sb.append("、");
                    sb.append(r.items.get(i).productName);
                }
            }
            h.tvProducts.setText(sb.length() > 0 ? sb.toString() : "订单#" + r.id);

            // Time
            h.tvTime.setText(r.saleTime != null ? r.saleTime : "");

            // Amount
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.CHINA);
            nf.setMaximumFractionDigits(0);
            h.tvAmount.setText("¥" + nf.format(r.totalAmount));

            // Qty
            h.tvQty.setText(r.itemCount + "件");
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvThumb, tvProducts, tvTime, tvAmount, tvQty;
            VH(View v) {
                super(v);
                tvThumb = v.findViewById(R.id.tv_order_thumb);
                tvProducts = v.findViewById(R.id.tv_order_products);
                tvTime = v.findViewById(R.id.tv_order_time);
                tvAmount = v.findViewById(R.id.tv_order_amount);
                tvQty = v.findViewById(R.id.tv_order_qty);
            }
        }
    }
}
