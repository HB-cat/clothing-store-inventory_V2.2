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
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.local.sync.OfflineHelper;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Product;
import com.heben.client.model.Models.ProductAttrCombination;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.util.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private java.util.List<Product> list;
    private Runnable onRefresh;

    public ProductAdapter(java.util.List<Product> list, Runnable onRefresh) {
        this.list = list;
        this.onRefresh = onRefresh;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product item = list.get(position);
        holder.tvName.setText(item.name);
        holder.tvPurchasePrice.setText("进价 ¥" + String.format("%.2f", item.purchasePrice));
        holder.tvSellingPrice.setText("售价 ¥" + String.format("%.2f", item.sellingPrice));

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("删除商品")
                    .setMessage("确定删除「" + item.name + "」？如有库存将清空")
                    .setPositiveButton("删除", (dialog, which) -> deleteProduct(v, item))
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void deleteProduct(View v, Product product) {
        if (!NetworkUtil.isConnected(v.getContext())) {
            Toast.makeText(v.getContext(), "离线模式暂不支持删除商品，请联网后操作", Toast.LENGTH_LONG).show();
            return;
        }

        // 先记录库存变动（如有组合库存）
        if (product.combinations != null && !product.combinations.isEmpty()) {
            OfflineHelper helper = new OfflineHelper(v.getContext());
            Long userId = TokenManager.getUserId();
            if (userId != null) {
                for (ProductAttrCombination combo : product.combinations) {
                    if (combo.stock > 0) {
                        helper.saveStockChangeOffline(userId, combo.id, -combo.stock);
                    }
                }
            }
        }

        ApiService api = ApiClient.getService();
        api.deleteProduct(product.id).enqueue(new Callback<ApiResponse<Void>>() {
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
        TextView tvName, tvPurchasePrice, tvSellingPrice, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_product_name);
            tvPurchasePrice = v.findViewById(R.id.tv_purchase_price);
            tvSellingPrice = v.findViewById(R.id.tv_selling_price);
            btnDelete = v.findViewById(R.id.btn_delete_product);
        }
    }
}
