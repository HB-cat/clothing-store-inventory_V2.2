package com.heben.client.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.adapter.PurchaseProductAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Product;
import com.heben.client.model.Models.PurchaseRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PurchaseActivity extends AppCompatActivity {

    private int currentTab = 1;

    // Tab views
    private TextView tab1, tab2, tab3;
    private View panel1, panel2, panel3;

    // Tab 1: my products
    private TextInputEditText etSearchProduct, etQty, etPriceInput;
    private RecyclerView rvProducts;
    private PurchaseProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private Product selectedProduct;

    // Tab 2: new product
    private TextInputEditText etNewName, etNewPurchasePrice, etNewSellingPrice, etNewQty;

    // Common
    private TextView tvSummary;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tab1 = findViewById(R.id.tab_my_products);
        tab2 = findViewById(R.id.tab_new_product);
        tab3 = findViewById(R.id.tab_wholesale);
        panel1 = findViewById(R.id.panel_my_products);
        panel2 = findViewById(R.id.panel_new_product);
        panel3 = findViewById(R.id.panel_wholesale);

        // Tab 1
        etSearchProduct = findViewById(R.id.et_search_product);
        rvProducts = findViewById(R.id.rv_purchase_products);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new PurchaseProductAdapter(productList, product -> {
            selectedProduct = product;
            etPriceInput.setText(String.format("%.2f", product.purchasePrice));
            updateSummary();
        });
        rvProducts.setAdapter(productAdapter);
        etQty = findViewById(R.id.et_purchase_qty);
        etPriceInput = findViewById(R.id.et_purchase_price_input);

        // Tab 2
        etNewName = findViewById(R.id.et_new_name);
        etNewPurchasePrice = findViewById(R.id.et_new_purchase_price);
        etNewSellingPrice = findViewById(R.id.et_new_selling_price);
        etNewQty = findViewById(R.id.et_new_qty);

        tvSummary = findViewById(R.id.tv_purchase_summary);
        btnConfirm = findViewById(R.id.btn_confirm_purchase);

        tab1.setOnClickListener(v -> switchTab(1));
        tab2.setOnClickListener(v -> switchTab(2));
        tab3.setOnClickListener(v -> switchTab(3));

        etSearchProduct.setOnEditorActionListener((v, actionId, event) -> {
            searchProducts();
            return false;
        });

        btnConfirm.setOnClickListener(v -> confirmPurchase());

        switchTab(1);
    }

    private void switchTab(int tab) {
        currentTab = tab;
        tab1.setSelected(tab == 1);
        tab2.setSelected(tab == 2);
        tab3.setSelected(tab == 3);
        panel1.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        panel2.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        panel3.setVisibility(tab == 3 ? View.VISIBLE : View.GONE);
        updateSummary();

        if (tab == 1 && productList.isEmpty()) {
            searchProducts();
        }
    }

    private void searchProducts() {
        String keyword = etSearchProduct.getText().toString().trim();
        ApiService api = ApiClient.getService();
        api.getProducts(null, keyword.isEmpty() ? null : keyword).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> data = response.body().getData();
                    productList.clear();
                    if (data != null) productList.addAll(data);
                    productAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {
                Toast.makeText(PurchaseActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary() {
        switch (currentTab) {
            case 1:
                String qty1 = etQty.getText().toString().trim();
                String price1 = etPriceInput.getText().toString().trim();
                if (selectedProduct != null && !qty1.isEmpty()) {
                    int q = Integer.parseInt(qty1);
                    double p = price1.isEmpty() ? selectedProduct.purchasePrice : Double.parseDouble(price1);
                    tvSummary.setText("已选 " + selectedProduct.name + " × " + q + "件  ¥" + String.format("%.2f", q * p));
                } else {
                    tvSummary.setText("请搜索并选择商品");
                }
                break;
            case 2:
                String qty2 = etNewQty.getText().toString().trim();
                String price2 = etNewPurchasePrice.getText().toString().trim();
                String name2 = etNewName.getText().toString().trim();
                if (!name2.isEmpty() && !qty2.isEmpty()) {
                    int q = Integer.parseInt(qty2);
                    double p = price2.isEmpty() ? 0 : Double.parseDouble(price2);
                    tvSummary.setText("新建 " + name2 + " × " + q + "件  ¥" + String.format("%.2f", q * p));
                } else {
                    tvSummary.setText("请填写商品信息");
                }
                break;
            case 3:
                tvSummary.setText("请输入搜索关键词查找共享商品");
                break;
        }
    }

    private void confirmPurchase() {
        btnConfirm.setEnabled(false);
        ApiService api = ApiClient.getService();
        Map<String, Object> body = new HashMap<>();

        switch (currentTab) {
            case 1:
                if (selectedProduct == null) {
                    Toast.makeText(this, "请先选择商品", Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    return;
                }
                String qtyStr = etQty.getText().toString().trim();
                String priceStr = etPriceInput.getText().toString().trim();
                if (qtyStr.isEmpty()) {
                    Toast.makeText(this, "请输入数量", Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    return;
                }
                body.put("productName", selectedProduct.name);
                body.put("quantity", Integer.parseInt(qtyStr));
                body.put("unitPrice", priceStr.isEmpty() ? selectedProduct.purchasePrice : Double.parseDouble(priceStr));
                api.purchaseMyProducts(body).enqueue(purchaseCallback);
                break;

            case 2:
                String name = etNewName.getText().toString().trim();
                String pPrice = etNewPurchasePrice.getText().toString().trim();
                String sPrice = etNewSellingPrice.getText().toString().trim();
                String qty2 = etNewQty.getText().toString().trim();
                if (name.isEmpty() || pPrice.isEmpty() || sPrice.isEmpty() || qty2.isEmpty()) {
                    Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    return;
                }
                body.put("productName", name);
                body.put("purchasePrice", Double.parseDouble(pPrice));
                body.put("sellingPrice", Double.parseDouble(sPrice));
                body.put("quantity", Integer.parseInt(qty2));
                api.purchaseNewProduct(body).enqueue(purchaseCallback);
                break;

            case 3:
                Toast.makeText(this, "批发商共享进货需联系批发商确认", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                break;
        }
    }

    private final Callback<ApiResponse<PurchaseRecord>> purchaseCallback = new Callback<>() {
        @Override
        public void onResponse(Call<ApiResponse<PurchaseRecord>> call, Response<ApiResponse<PurchaseRecord>> response) {
            btnConfirm.setEnabled(true);
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                Toast.makeText(PurchaseActivity.this, "入库成功", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String msg = response.body() != null ? response.body().getMessage() : "入库失败";
                Toast.makeText(PurchaseActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(Call<ApiResponse<PurchaseRecord>> call, Throwable t) {
            btnConfirm.setEnabled(true);
            Toast.makeText(PurchaseActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };
}
