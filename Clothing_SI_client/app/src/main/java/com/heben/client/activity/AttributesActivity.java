package com.heben.client.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.adapter.AttributeGroupAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.AttributeGroup;
import com.heben.client.model.Models.AttributeValue;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttributesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private AttributeGroupAdapter adapter;
    private List<AttributeGroup> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attributes);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_attribute_groups);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AttributeGroupAdapter(list, new AttributeGroupAdapter.OnGroupAction() {
            @Override
            public void onDeleteGroup(int position) {
                AttributeGroup g = list.get(position);
                new AlertDialog.Builder(AttributesActivity.this)
                        .setTitle("删除属性组")
                        .setMessage("确定删除「" + g.name + "」？")
                        .setPositiveButton("删除", (d, w) ->
                                ApiClient.getService().deleteAttributeGroup(g.id).enqueue(new Callback<>() {
                                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) load();
                                    }
                                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                                })
                        ).setNegativeButton("取消", null).show();
            }

            @Override
            public void onAddValue(int position) {
                AttributeGroup g = list.get(position);
                EditText input = new EditText(AttributesActivity.this);
                new AlertDialog.Builder(AttributesActivity.this)
                        .setTitle("添加属性值 - " + g.name)
                        .setView(input)
                        .setPositiveButton("添加", (d, w) -> {
                            String val = input.getText().toString().trim();
                            if (val.isEmpty()) return;
                            HashMap<String, Object> body = new HashMap<>();
                            body.put("groupId", g.id);
                            body.put("value", val);
                            ApiClient.getService().addAttributeValue(body).enqueue(new Callback<ApiResponse<AttributeValue>>() {
                                @Override public void onResponse(Call<ApiResponse<AttributeValue>> call, Response<ApiResponse<AttributeValue>> resp) {
                                    if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) load();
                                }
                                @Override public void onFailure(Call<ApiResponse<AttributeValue>> call, Throwable t) {}
                            });
                        }).setNegativeButton("取消", null).show();
            }

            @Override
            public void onDeleteValue(int groupPos, int valuePos) {
                AttributeValue av = list.get(groupPos).values.get(valuePos);
                ApiClient.getService().deleteAttributeValue(av.id).enqueue(new Callback<>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) load();
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
            }
        });
        rv.setAdapter(adapter);
        load();

        findViewById(R.id.btn_add_group).setOnClickListener(v -> {
            EditText input = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("新建属性组")
                    .setView(input)
                    .setPositiveButton("创建", (d, w) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) return;
                        HashMap<String, String> body = new HashMap<>();
                        body.put("name", name);
                        ApiClient.getService().addAttributeGroup(body).enqueue(new Callback<>() {
                            @Override public void onResponse(Call<ApiResponse<AttributeGroup>> call, Response<ApiResponse<AttributeGroup>> resp) {
                                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) load();
                            }
                            @Override public void onFailure(Call<ApiResponse<AttributeGroup>> call, Throwable t) {}
                        });
                    }).setNegativeButton("取消", null).show();
        });
    }

    private void load() {
        ApiClient.getService().getAttributeGroups().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AttributeGroup>>> call, Response<ApiResponse<List<AttributeGroup>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    list.clear();
                    List<AttributeGroup> data = response.body().getData();
                    if (data != null) list.addAll(data);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<AttributeGroup>>> call, Throwable t) {}
        });
    }
}
