package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Category;
import com.heben.client.model.Models.Product;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAddActivity extends AppCompatActivity {

    private TextInputEditText etName, etPurchasePrice, etSellingPrice;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_add);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_product_name);
        etPurchasePrice = findViewById(R.id.et_purchase_price);
        etSellingPrice = findViewById(R.id.et_selling_price);
        btnSave = findViewById(R.id.btn_save_product);

        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String purchaseStr = etPurchasePrice.getText().toString().trim();
        String sellingStr = etSellingPrice.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (purchaseStr.isEmpty()) {
            Toast.makeText(this, "请输入进价", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sellingStr.isEmpty()) {
            Toast.makeText(this, "请输入售价", Toast.LENGTH_SHORT).show();
            return;
        }

        double purchasePrice = Double.parseDouble(purchaseStr);
        double sellingPrice = Double.parseDouble(sellingStr);

        btnSave.setEnabled(false);

        // Get first category id to assign
        ApiService api = ApiClient.getService();
        api.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                long categoryId = 0;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> cats = response.body().getData();
                    if (cats != null && !cats.isEmpty()) {
                        categoryId = cats.get(0).id;
                    }
                }
                doAddProduct(name, purchasePrice, sellingPrice, categoryId);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                doAddProduct(name, purchasePrice, sellingPrice, 0);
            }
        });
    }

    private void doAddProduct(String name, double purchasePrice, double sellingPrice, long categoryId) {
        ApiService api = ApiClient.getService();
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("categoryId", categoryId);
        body.put("purchasePrice", purchasePrice);
        body.put("sellingPrice", sellingPrice);

        api.addProduct(body).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ProductAddActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "添加失败";
                    Toast.makeText(ProductAddActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(ProductAddActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
