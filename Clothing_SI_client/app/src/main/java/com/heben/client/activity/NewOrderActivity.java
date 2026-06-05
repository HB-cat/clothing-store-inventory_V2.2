package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.adapter.DraftBillAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.DraftBill;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOrderActivity extends AppCompatActivity {

    private RecyclerView rv;
    private DraftBillAdapter adapter;
    private List<DraftBill> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_drafts);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DraftBillAdapter(list, new DraftBillAdapter.OnDraftAction() {
            @Override
            public void onEdit(int position) {
                DraftBill d = list.get(position);
                Intent intent = new Intent(NewOrderActivity.this, NewOrderEditActivity.class);
                intent.putExtra("draftId", d.id);
                startActivity(intent);
            }

            @Override
            public void onDelete(int position) {
                DraftBill d = list.get(position);
                ApiClient.getService().deleteDraft(d.id).enqueue(new Callback<>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) load();
                        else Toast.makeText(NewOrderActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }

            @Override
            public void onSubmit(int position) {
                DraftBill d = list.get(position);
                ApiClient.getService().submitDraft(d.id).enqueue(new Callback<>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                            Toast.makeText(NewOrderActivity.this, "提交成功", Toast.LENGTH_SHORT).show();
                            load();
                        } else Toast.makeText(NewOrderActivity.this, "提交失败", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btn_create_draft).setOnClickListener(v -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("buyerName", "");
            body.put("items", new ArrayList<>());
            ApiClient.getService().createDraft(body).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<DraftBill>> call, Response<ApiResponse<DraftBill>> resp) {
                    if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                        DraftBill d = resp.body().getData();
                        Intent intent = new Intent(NewOrderActivity.this, NewOrderEditActivity.class);
                        intent.putExtra("draftId", d.id);
                        startActivity(intent);
                    } else Toast.makeText(NewOrderActivity.this, "创建失败", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(Call<ApiResponse<DraftBill>> call, Throwable t) {}
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.getService().getDrafts().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DraftBill>>> call, Response<ApiResponse<List<DraftBill>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    list.clear();
                    List<DraftBill> data = response.body().getData();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<DraftBill>>> call, Throwable t) {}
        });
    }
}
