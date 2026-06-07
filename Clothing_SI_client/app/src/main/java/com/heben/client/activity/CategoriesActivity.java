package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.adapter.CategoryAdapter;
import com.heben.client.local.AppDatabase;
import com.heben.client.local.entity.LocalCategory;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Category;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.util.SoundManager;
import com.heben.client.util.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CategoryAdapter adapter;
    private List<Category> fullList = new ArrayList<>();
    private TextInputEditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Top + button
        findViewById(R.id.btn_add_category).setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("添加分类");
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("分类名称");
            input.setPadding(40, 20, 40, 20);
            builder.setView(input);
            builder.setPositiveButton("确定", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) addCategory(name);
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        });

        etSearch = findViewById(R.id.et_search_category);
        rv = findViewById(R.id.rv_categories);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CategoryAdapter(new ArrayList<>(), () -> loadCategories());
        rv.setAdapter(adapter);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            filterCategories(etSearch.getText().toString().trim());
            return true;
        });

        loadCategories();
    }

    private void loadCategories() {
        // 1. 先从本地 Room 加载（后台线程，不阻塞 UI）
        Long uid = TokenManager.getUserId();
        if (uid != null) {
            new Thread(() -> {
                List<LocalCategory> localCats = AppDatabase.getInstance(this).categoryDao().getAll(uid);
                if (!localCats.isEmpty()) {
                    List<Category> cats = new ArrayList<>();
                    for (LocalCategory lc : localCats) {
                        Category c = new Category();
                        c.id = lc.id;
                        c.name = lc.name;
                        c.sortOrder = lc.sortOrder;
                        c.isPreset = lc.isPreset;
                        cats.add(c);
                    }
                    runOnUiThread(() -> {
                        fullList.clear();
                        fullList.addAll(cats);
                        adapter.updateList(new ArrayList<>(fullList));
                    });
                }
            }).start();
        }

        // 2. 后台尝试从网络刷新
        ApiService api = ApiClient.getService();
        api.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> data = response.body().getData();
                    fullList.clear();
                    if (data != null) fullList.addAll(data);
                    adapter.updateList(new ArrayList<>(fullList));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                // 已从本地加载，网络失败不提示
            }
        });
    }

    private void filterCategories(String keyword) {
        List<Category> filtered = new ArrayList<>();
        for (Category c : fullList) {
            if (keyword.isEmpty() || c.name.toLowerCase().contains(keyword.toLowerCase())) {
                filtered.add(c);
            }
        }
        adapter.updateList(filtered);
    }

    private void addCategory(String name) {
        ApiService api = ApiClient.getService();
        HashMap<String, String> body = new HashMap<>();
        body.put("name", name);
        api.addCategory(body).enqueue(new Callback<ApiResponse<Category>>() {
            @Override
            public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SoundManager.getInstance(CategoriesActivity.this).playClick();
                    Toast.makeText(CategoriesActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else {
                    Toast.makeText(CategoriesActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                Toast.makeText(CategoriesActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
