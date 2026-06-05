package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SaleActivity extends AppCompatActivity {

    private TextInputEditText etName, etQuantity, etPrice;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale);

        etName = findViewById(R.id.et_product_name);
        etQuantity = findViewById(R.id.et_quantity);
        etPrice = findViewById(R.id.et_price);
        btnSubmit = findViewById(R.id.btn_submit_sale);

        btnSubmit.setOnClickListener(v -> quickSale());
    }

    private void quickSale() {
        String name = etName.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "请输入数量", Toast.LENGTH_SHORT).show();
            return;
        }
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "请输入单价", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(qtyStr);
        double price = Double.parseDouble(priceStr);
        btnSubmit.setEnabled(false);

        ApiService api = ApiClient.getService();
        Map<String, Object> body = new HashMap<>();
        body.put("productName", name);
        body.put("quantity", quantity);
        body.put("unitPrice", price);

        api.quickSale(body).enqueue(new Callback<ApiResponse<SaleRecord>>() {
            @Override
            public void onResponse(Call<ApiResponse<SaleRecord>> call, Response<ApiResponse<SaleRecord>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(SaleActivity.this, "记账成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "记账失败";
                    Toast.makeText(SaleActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SaleRecord>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(SaleActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
