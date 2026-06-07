package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.local.entity.LocalSaleRecordItem;
import com.heben.client.local.sync.OfflineHelper;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SaleRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.util.SoundManager;
import com.heben.client.util.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SaleActivity extends AppCompatActivity {

    private TextInputEditText etPrice, etNote;
    private AppCompatSpinner spinnerBuyer;
    private TextView tvQty, tvSelectedProduct, tvProductStock;
    private int quantity = 1;
    private MaterialButton btnSubmit;
    private OfflineHelper offlineHelper;
    private String selectedProductName = "";
    private String selectedProductEmoji = "👕";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale);

        offlineHelper = new OfflineHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etPrice = findViewById(R.id.et_price);
        spinnerBuyer = findViewById(R.id.spinner_buyer);
        etNote = findViewById(R.id.et_note);
        tvQty = findViewById(R.id.tv_qty);
        tvSelectedProduct = findViewById(R.id.tv_selected_product);
        tvProductStock = findViewById(R.id.tv_product_stock);
        btnSubmit = findViewById(R.id.btn_submit_sale);

        // 数量步进器
        findViewById(R.id.btn_qty_minus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQty.setText(String.valueOf(quantity));
            }
        });
        findViewById(R.id.btn_qty_plus).setOnClickListener(v -> {
            quantity++;
            tvQty.setText(String.valueOf(quantity));
        });

        // 商品选择（跳转商品列表选商品）
        findViewById(R.id.layout_product_selector).setOnClickListener(v -> {
            // TODO: 打开商品选择列表
            Toast.makeText(this, "选择商品", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> quickSale());
    }

    private void quickSale() {
        String name = tvSelectedProduct.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请选择商品", Toast.LENGTH_SHORT).show();
            return;
        }
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "请输入实付金额", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        double unitPrice = quantity > 0 ? price / quantity : price;
        btnSubmit.setEnabled(false);

        if (offlineHelper.isOnline()) {
            callQuickSaleApi(name, quantity, unitPrice);
        } else {
            saveSaleOffline(name, quantity, unitPrice);
        }
    }

    private void callQuickSaleApi(String name, int qty, double unitPrice) {
        ApiService api = ApiClient.getService();
        Map<String, Object> body = new HashMap<>();
        body.put("productName", name);
        body.put("quantity", qty);
        body.put("unitPrice", unitPrice);

        api.quickSale(body).enqueue(new Callback<ApiResponse<SaleRecord>>() {
            @Override
            public void onResponse(Call<ApiResponse<SaleRecord>> call, Response<ApiResponse<SaleRecord>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SoundManager.getInstance(SaleActivity.this).playSaleSuccess();
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
                saveSaleOffline(name, qty, unitPrice);
            }
        });
    }

    private void saveSaleOffline(String name, int qty, double unitPrice) {
        Long userId = TokenManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "未登录", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            return;
        }

        String saleTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()).format(new Date());
        double totalAmount = qty * unitPrice;

        List<LocalSaleRecordItem> items = new ArrayList<>();
        LocalSaleRecordItem item = new LocalSaleRecordItem();
        item.productName = name;
        item.quantity = qty;
        item.unitPrice = unitPrice;
        items.add(item);

        offlineHelper.saveSaleOffline(userId, "", totalAmount, 1, saleTime, items);

        btnSubmit.setEnabled(true);
        Toast.makeText(this, "记账已保存到本地，网络恢复后自动同步", Toast.LENGTH_LONG).show();
        SoundManager.getInstance(this).playSaleSuccess();
        finish();
    }
}
