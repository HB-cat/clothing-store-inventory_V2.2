package com.heben.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Category;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> list;
    private Runnable onRefresh;

    public CategoryAdapter(List<Category> list, Runnable onRefresh) {
        this.list = list;
        this.onRefresh = onRefresh;
    }

    public void updateList(List<Category> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = list.get(position);
        holder.tvName.setText(item.name);
        if (item.isPreset) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        // Click to rename
        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("重命名分类");
            androidx.appcompat.widget.AppCompatEditText et = new androidx.appcompat.widget.AppCompatEditText(v.getContext());
            et.setText(item.name);
            et.setSelection(et.getText().length());
            builder.setView(et, 40, 0, 40, 0);
            builder.setPositiveButton("确定", (dialog, which) -> {
                String newName = et.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(item.name)) {
                    renameCategory(v, item.id, newName);
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        });

        // Delete
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("删除分类")
                    .setMessage("确定删除「" + item.name + "」？")
                    .setPositiveButton("删除", (dialog, which) -> deleteCategory(v, item.id))
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void renameCategory(View v, long id, String newName) {
        ApiService api = ApiClient.getService();
        HashMap<String, String> body = new HashMap<>();
        body.put("name", newName);
        api.updateCategory(id, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(v.getContext(), "重命名成功", Toast.LENGTH_SHORT).show();
                    onRefresh.run();
                } else {
                    Toast.makeText(v.getContext(), "重命名失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(v.getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCategory(View v, long id) {
        ApiService api = ApiClient.getService();
        api.deleteCategory(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(v.getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                    onRefresh.run();
                } else {
                    Toast.makeText(v.getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(v.getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_category_name);
            btnDelete = v.findViewById(R.id.btn_delete_category);
        }
    }
}
