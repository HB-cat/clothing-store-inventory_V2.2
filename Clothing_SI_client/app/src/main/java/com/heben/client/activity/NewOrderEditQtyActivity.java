package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOrderEditQtyActivity extends AppCompatActivity {

    private int quantity = 1;
    private double unitPrice = 65.0;
    private TextView tvQty, tvSubtotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order_edit_qty);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Setup
        long draftId = getIntent().getLongExtra("draftId", 0);
        long itemId = getIntent().getLongExtra("itemId", 0);
        String productName = getIntent().getStringExtra("productName");
        quantity = getIntent().getIntExtra("currentQty", 1);

        TextView tvProductName = findViewById(R.id.tv_product_name);
        tvQty = findViewById(R.id.tv_qty);
        tvSubtotal = findViewById(R.id.tv_subtotal);

        if (productName != null) tvProductName.setText(productName);
        tvQty.setText(String.valueOf(quantity));
        updateSubtotal();

        // 步进器
        findViewById(R.id.btn_qty_minus).setOnClickListener(v -> {
            if (quantity > 1) { quantity--; updateDisplay(); }
        });
        findViewById(R.id.btn_qty_plus).setOnClickListener(v -> {
            quantity++; updateDisplay();
        });

        MaterialButton btnConfirm = findViewById(R.id.btn_confirm_qty);
        btnConfirm.setOnClickListener(v -> {
            SoundManager.getInstance(NewOrderEditQtyActivity.this).playSuccess();
            Toast.makeText(NewOrderEditQtyActivity.this, "已更新", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateDisplay() {
        tvQty.setText(String.valueOf(quantity));
        updateSubtotal();
    }

    private void updateSubtotal() {
        double subtotal = quantity * unitPrice;
        tvSubtotal.setText("¥" + String.format("%.0f", subtotal));
    }
}
