package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.adapter.SharedProductAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SharedProduct;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerProductsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private SharedProductAdapter adapter;
    private List<SharedProduct> list = new ArrayList<>();
    private String keyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_products);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        keyword = getIntent().getStringExtra("keyword");
        if (keyword == null) keyword = "";

        rv = findViewById(R.id.rv_buyer_products);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SharedProductAdapter(list, position -> {
            SharedProduct p = list.get(position);
            HashMap<String, Object> body = new HashMap<>();
            body.put("sharedProductId", p.id);
            body.put("productName", p.productName);
            body.put("quantity", 1);
            body.put("buyerNote", "买家批发请求");
            ApiClient.getService().wholesaleRequest(body).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(BuyerProductsActivity.this, "批发请求已发送", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BuyerProductsActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(BuyerProductsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
        rv.setAdapter(adapter);
        load();
    }

    private void load() {
        ApiClient.getService().getAvailableSharedProducts(keyword).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SharedProduct>>> call, Response<ApiResponse<List<SharedProduct>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    list.clear();
                    List<SharedProduct> data = response.body().getData();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<SharedProduct>>> call, Throwable t) {}
        });
    }
}
