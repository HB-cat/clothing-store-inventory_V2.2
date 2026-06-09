package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.adapter.ProductAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Product;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.util.SoundManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ProductAdapter adapter;
    private List<Product> list = new ArrayList<>();
    private TextInputEditText etSearch;
    private View tabAll, tabOnsale, tabOffline, tabLowStock;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });
        findViewById(R.id.btn_add_product).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(this, ProductAddActivity.class));
        });

        // Segment
        tabAll = findViewById(R.id.tab_all);
        tabOnsale = findViewById(R.id.tab_onsale);
        tabOffline = findViewById(R.id.tab_offline);
        tabLowStock = findViewById(R.id.tab_low_stock);

        tabAll.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab("all");
        });
        tabOnsale.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab("onsale");
        });
        tabOffline.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab("offline");
        });
        tabLowStock.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab("low_stock");
        });

        etSearch = findViewById(R.id.et_search);
        rv = findViewById(R.id.product_list);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProductAdapter(list, () -> loadProducts(currentFilter));
        rv.setAdapter(adapter);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = etSearch.getText().toString().trim();
            loadProducts(currentFilter);
            return true;
        });

        switchTab("all");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(currentFilter);
    }

    private void switchTab(String tab) {
        currentFilter = tab;
        tabAll.setSelected("all".equals(tab));
        tabOnsale.setSelected("onsale".equals(tab));
        tabOffline.setSelected("offline".equals(tab));
        tabLowStock.setSelected("low_stock".equals(tab));
        loadProducts(tab);
    }

    private void loadProducts(String filter) {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) keyword = null;

        ApiService api = ApiClient.getService();
        api.getProducts(null, keyword).enqueue(new Callback<ApiResponse<List<Product>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> data = response.body().getData();
                    list.clear();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {
                Toast.makeText(ProductsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
