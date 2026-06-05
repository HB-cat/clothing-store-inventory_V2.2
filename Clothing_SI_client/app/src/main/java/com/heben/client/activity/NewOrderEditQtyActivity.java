package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.network.ApiClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOrderEditQtyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order_edit_qty);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        long draftId = getIntent().getLongExtra("draftId", 0);
        long itemId = getIntent().getLongExtra("itemId", 0);
        String productName = getIntent().getStringExtra("productName");
        int currentQty = getIntent().getIntExtra("currentQty", 1);

        TextInputEditText etProduct = findViewById(R.id.et_item_product);
        TextInputEditText etQty = findViewById(R.id.et_item_qty);
        MaterialButton btnConfirm = findViewById(R.id.btn_confirm_qty);

        if (productName != null) etProduct.setText(productName);
        etQty.setText(String.valueOf(currentQty));

        btnConfirm.setOnClickListener(v -> {
            String qtyStr = etQty.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                Toast.makeText(this, "请输入数量", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty = Integer.parseInt(qtyStr);

            HashMap<String, Object> body = new HashMap<>();
            body.put("itemId", itemId);
            body.put("quantity", qty);

            ApiClient.getService().updateDraft(draftId, body).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(NewOrderEditQtyActivity.this, "已更新", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(NewOrderEditQtyActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
            });
        });
    }
}
