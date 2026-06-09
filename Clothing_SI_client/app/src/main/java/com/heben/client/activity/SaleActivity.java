package com.heben.client.activity;

import android.app.AlertDialog;
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
import com.heben.client.agent.AgentSuggestionManager;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Product;
import com.heben.client.model.Models.ProductAttrCombination;
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
    private TextView tvQty, tvSelectedProduct;
    private int quantity = 1;
    private MaterialButton btnSubmit;
    private OfflineHelper offlineHelper;
    private String selectedProductName = "";
    private long selectedCombinationId = 0;

    /** 本地缓存的商品列表（启动时加载一次） */
    private List<Product> cachedProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale);

        offlineHelper = new OfflineHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        etPrice = findViewById(R.id.et_price);
        spinnerBuyer = findViewById(R.id.spinner_buyer);
        etNote = findViewById(R.id.et_note);
        tvQty = findViewById(R.id.tv_qty);
        tvSelectedProduct = findViewById(R.id.tv_selected_product);
        btnSubmit = findViewById(R.id.btn_submit_sale);

        // 数量步进器
        findViewById(R.id.btn_qty_minus).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            if (quantity > 1) {
                quantity--;
                tvQty.setText(String.valueOf(quantity));
            }
        });
        findViewById(R.id.btn_qty_plus).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            quantity++;
            tvQty.setText(String.valueOf(quantity));
        });

        // 商品选择
        findViewById(R.id.layout_product_selector).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            showProductPicker();
        });

        btnSubmit.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            quickSale();
        });

        // 启动时加载一次商品列表
        loadProducts();
    }

    /** 加载商品列表到本地缓存 */
    private void loadProducts() {
        if (!offlineHelper.isOnline()) return;
        ApiClient.getService().getProducts(null, null).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> data = response.body().getData();
                    if (data != null) cachedProducts = data;
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {}
        });
    }

    /** 弹出商品选择对话框 */
    private void showProductPicker() {
        if (cachedProducts.isEmpty()) {
            loadProducts();
            Toast.makeText(this, "商品列表加载中，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[cachedProducts.size()];
        for (int i = 0; i < cachedProducts.size(); i++) {
            Product p = cachedProducts.get(i);
            names[i] = p.name + "  ¥" + String.format("%.0f", p.sellingPrice);
        }

        new AlertDialog.Builder(this)
                .setTitle("选择商品")
                .setItems(names, (dialog, which) -> {
                    Product p = cachedProducts.get(which);
                    selectedProductName = p.name;
                    // 取第一个属性组合的 ID 作为 combinationId
                    selectedCombinationId = getDefaultCombinationId(p);
                    tvSelectedProduct.setText(p.name);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 获取商品默认属性组合 ID */
    private long getDefaultCombinationId(Product product) {
        if (product.combinations != null && !product.combinations.isEmpty()) {
            ProductAttrCombination c = product.combinations.get(0);
            return c.id != null ? c.id : 0;
        }
        return 0;
    }

    private void quickSale() {
        String priceStr = etPrice.getText().toString().trim();

        if (selectedProductName.isEmpty()) {
            showProductPicker();
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
            callQuickSaleApi(quantity, unitPrice);
        } else {
            saveSaleOffline(quantity, unitPrice);
        }
    }

    private void callQuickSaleApi(int qty, double unitPrice) {
        // 取买家
        String buyerName = getBuyerName();

        // 构造后端要求的格式
        Map<String, Object> body = new HashMap<>();
        body.put("buyerName", buyerName);
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("combinationId", selectedCombinationId);
        item.put("quantity", qty);
        item.put("unitPrice", unitPrice);
        items.add(item);
        body.put("items", items);

        ApiClient.getService().quickSale(body).enqueue(new Callback<ApiResponse<SaleRecord>>() {
            @Override
            public void onResponse(Call<ApiResponse<SaleRecord>> call, Response<ApiResponse<SaleRecord>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SoundManager.getInstance(SaleActivity.this).playSaleSuccess();
                    Toast.makeText(SaleActivity.this, "记账成功", Toast.LENGTH_SHORT).show();
                    AgentSuggestionManager.recordSale();
                    AgentSuggestionManager.recordProductUsed(selectedProductName);
                    AgentSuggestionManager.recordSalePrice(selectedProductName, unitPrice);
                    finish();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "记账失败";
                    Toast.makeText(SaleActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SaleRecord>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                saveSaleOffline(qty, unitPrice);
            }
        });
    }

    /** 从 Spinner 获取买家名称（去掉括号备注） */
    private String getBuyerName() {
        String raw = spinnerBuyer.getSelectedItem() != null
                ? spinnerBuyer.getSelectedItem().toString() : "散客（不记名）";
        int idx = raw.indexOf("（");
        return idx > 0 ? raw.substring(0, idx) : raw;
    }

    private void saveSaleOffline(int qty, double unitPrice) {
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
        item.productName = selectedProductName;
        item.quantity = qty;
        item.unitPrice = unitPrice;
        items.add(item);

        offlineHelper.saveSaleOffline(userId, "", totalAmount, 1, saleTime, items);

        AgentSuggestionManager.recordSale();
        AgentSuggestionManager.recordProductUsed(selectedProductName);
        AgentSuggestionManager.recordSalePrice(selectedProductName, unitPrice);

        btnSubmit.setEnabled(true);
        Toast.makeText(this, "记账已保存到本地，网络恢复后自动同步", Toast.LENGTH_LONG).show();
        SoundManager.getInstance(this).playSaleSuccess();
        finish();
    }
}
