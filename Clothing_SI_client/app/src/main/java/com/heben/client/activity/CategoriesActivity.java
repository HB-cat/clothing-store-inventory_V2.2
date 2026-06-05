package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.adapter.CategoryAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Category;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CategoryAdapter adapter;
    private List<Category> list = new ArrayList<>();
    private TextInputEditText etName;
    private MaterialButton btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_category_name);
        btnAdd = findViewById(R.id.btn_add_category);
        rv = findViewById(R.id.rv_categories);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CategoryAdapter(list, this::loadCategories);
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addCategory());

        loadCategories();
    }

    private void loadCategories() {
        ApiService api = ApiClient.getService();
        api.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> data = response.body().getData();
                    list.clear();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Toast.makeText(CategoriesActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCategory() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
            return;
        }
        btnAdd.setEnabled(false);

        ApiService api = ApiClient.getService();
        HashMap<String, String> body = new HashMap<>();
        body.put("name", name);
        api.addCategory(body).enqueue(new Callback<ApiResponse<Category>>() {
            @Override
            public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                btnAdd.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    etName.setText("");
                    Toast.makeText(CategoriesActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else {
                    Toast.makeText(CategoriesActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                btnAdd.setEnabled(true);
                Toast.makeText(CategoriesActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
