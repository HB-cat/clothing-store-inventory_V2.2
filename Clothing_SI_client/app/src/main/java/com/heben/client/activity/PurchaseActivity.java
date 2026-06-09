package com.heben.client.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.agent.AgentSuggestionManager;
import com.heben.client.adapter.PurchaseProductAdapter;
import com.heben.client.local.entity.LocalPurchaseRecordItem;
import com.heben.client.local.sync.OfflineHelper;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.Category;
import com.heben.client.model.Models.Product;
import com.heben.client.model.Models.ProductAttrCombination;
import com.heben.client.model.Models.PurchaseRecord;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.util.SoundManager;
import com.heben.client.util.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PurchaseActivity extends AppCompatActivity {

    private int currentTab = 1;

    private TextView tab1, tab2, tab3;
    private View panel1, panel2, panel3;

    private TextInputEditText etSearchProduct, etQty, etPriceInput;
    private RecyclerView rvProducts;
    private PurchaseProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private Product selectedProduct;

    private TextInputEditText etNewName, etNewPurchasePrice, etNewSellingPrice, etNewQty;
    private TextView tvNewCategory;
    private List<Category> categoryList = new ArrayList<>();
    private Category selectedCategory;

    private TextView tvSummary;
    private MaterialButton btnConfirm;

    private OfflineHelper offlineHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        offlineHelper = new OfflineHelper(this);

        tab1 = findViewById(R.id.tab_my_products);
        tab2 = findViewById(R.id.tab_new_product);
        tab3 = findViewById(R.id.tab_wholesale);
        panel1 = findViewById(R.id.panel_my_products);
        panel2 = findViewById(R.id.panel_new_product);
        panel3 = findViewById(R.id.panel_wholesale);

        etSearchProduct = findViewById(R.id.et_search_product);
        rvProducts = findViewById(R.id.rv_purchase_products);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new PurchaseProductAdapter(productList, product -> {
            SoundManager.getInstance(this).playClick();
            selectedProduct = product;
            etPriceInput.setText(String.format("%.2f", product.purchasePrice));
            updateSummary();
        });
        rvProducts.setAdapter(productAdapter);
        etQty = findViewById(R.id.et_purchase_qty);
        etPriceInput = findViewById(R.id.et_purchase_price_input);

        // 输入时自动更新摘要
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSummary(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etQty.addTextChangedListener(tw);
        etPriceInput.addTextChangedListener(tw);

        etNewName = findViewById(R.id.et_new_name);
        etNewPurchasePrice = findViewById(R.id.et_new_purchase_price);
        etNewSellingPrice = findViewById(R.id.et_new_selling_price);
        etNewQty = findViewById(R.id.et_new_qty);
        tvNewCategory = findViewById(R.id.tv_new_category);

        // Tab2 分类选择
        tvNewCategory.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            showCategoryPicker();
        });

        // 新建Tab输入自动更新摘要
        TextWatcher tw2 = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSummary(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etNewName.addTextChangedListener(tw2);
        etNewPurchasePrice.addTextChangedListener(tw2);
        etNewSellingPrice.addTextChangedListener(tw2);
        etNewQty.addTextChangedListener(tw2);

        tvSummary = findViewById(R.id.tv_purchase_summary);
        btnConfirm = findViewById(R.id.btn_confirm_purchase);

        tab1.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(1);
        });
        tab2.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(2);
        });
        tab3.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            switchTab(3);
        });

        // 搜索商品 — 实时搜索（防抖）
        etSearchProduct.addTextChangedListener(new TextWatcher() {
            private android.os.Handler handler = new android.os.Handler();
            private Runnable searchTask;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTask != null) handler.removeCallbacks(searchTask);
                searchTask = () -> searchProducts();
                handler.postDelayed(searchTask, 500);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            confirmPurchase();
        });

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
        if (tab == 2 && categoryList.isEmpty()) {
            loadCategories();
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

    /** 加载分类列表（Tab2 用） */
    private void loadCategories() {
        ApiClient.getService().getCategories().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> data = response.body().getData();
                    categoryList.clear();
                    if (data != null) categoryList.addAll(data);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {}
        });
    }

    /** 弹出分类选择器 */
    private void showCategoryPicker() {
        if (categoryList.isEmpty()) {
            Toast.makeText(this, "暂无分类，请先在商品管理中创建", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[categoryList.size()];
        for (int i = 0; i < categoryList.size(); i++) {
            names[i] = categoryList.get(i).name;
        }
        new AlertDialog.Builder(this)
                .setTitle("选择分类")
                .setItems(names, (dialog, which) -> {
                    selectedCategory = categoryList.get(which);
                    tvNewCategory.setText("分类：" + selectedCategory.name);
                    updateSummary();
                })
                .setNegativeButton("取消", null)
                .show();
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
                } else if (selectedProduct != null) {
                    tvSummary.setText("已选 " + selectedProduct.name + "，请输入数量");
                } else {
                    tvSummary.setText(productList.isEmpty() ? "搜索商品并点击选择" : "请点击选择下方商品");
                }
                break;
            case 2:
                String name2 = etNewName.getText().toString().trim();
                String qty2 = etNewQty.getText().toString().trim();
                String price2 = etNewPurchasePrice.getText().toString().trim();
                if (!name2.isEmpty() && !qty2.isEmpty()) {
                    int q = Integer.parseInt(qty2);
                    double p = price2.isEmpty() ? 0 : Double.parseDouble(price2);
                    String cat = selectedCategory != null ? " [" + selectedCategory.name + "]" : "";
                    tvSummary.setText("新建 " + name2 + cat + " × " + q + "件  ¥" + String.format("%.2f", q * p));
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

        if (currentTab == 1) {
            // —— Tab 1: 从我的商品补货 ——
            if (selectedProduct == null) {
                Toast.makeText(this, "请先搜索并选择商品", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                return;
            }
            String qtyStr = etQty.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                Toast.makeText(this, "请输入数量", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                return;
            }
            int quantity = Integer.parseInt(qtyStr);

            // 获取 combinationId：使用第一个属性组合
            Long combinationId = getDefaultCombinationId(selectedProduct);
            if (combinationId == null) {
                Toast.makeText(this, "该商品没有可入库的属性组合", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                return;
            }

            AgentSuggestionManager.recordPurchase();
            AgentSuggestionManager.recordProductUsed(selectedProduct.name);
            AgentSuggestionManager.recordPurchaseCycle(selectedProduct.name);

            if (offlineHelper.isOnline()) {
                Map<String, Object> body = new HashMap<>();
                body.put("productId", selectedProduct.id);
                body.put("combinationId", combinationId);
                body.put("quantity", quantity);
                ApiClient.getService().purchaseMyProducts(body).enqueue(purchaseCallback);
            } else {
                savePurchaseOffline(1, selectedProduct.name, quantity, selectedProduct.purchasePrice);
            }

        } else if (currentTab == 2) {
            // —— Tab 2: 新建商品并入库 ——
            String name = etNewName.getText().toString().trim();
            String pPrice = etNewPurchasePrice.getText().toString().trim();
            String sPrice = etNewSellingPrice.getText().toString().trim();
            String qtyStr = etNewQty.getText().toString().trim();

            if (name.isEmpty() || pPrice.isEmpty() || sPrice.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                return;
            }
            if (selectedCategory == null) {
                Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                return;
            }

            int quantity = Integer.parseInt(qtyStr);

            AgentSuggestionManager.recordPurchase();
            AgentSuggestionManager.recordProductUsed(name);
            AgentSuggestionManager.recordPurchaseCycle(name);

            // 构造 AddProductRequest 格式
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("categoryId", selectedCategory.id);
            body.put("purchasePrice", Double.parseDouble(pPrice));
            body.put("sellingPrice", Double.parseDouble(sPrice));
            List<Map<String, Object>> combos = new ArrayList<>();
            Map<String, Object> combo = new HashMap<>();
            combo.put("attrsJson", "{}");
            combo.put("stock", quantity);
            combos.add(combo);
            body.put("combinations", combos);

            ApiClient.getService().purchaseNewProduct(body).enqueue(purchaseCallback);

        } else {
            btnConfirm.setEnabled(true);
            Toast.makeText(this, "批发商共享进货需联系批发商确认", Toast.LENGTH_SHORT).show();
        }
    }

    /** 获取商品默认属性组合 ID（取第一个） */
    private Long getDefaultCombinationId(Product product) {
        if (product.combinations != null && !product.combinations.isEmpty()) {
            ProductAttrCombination c = product.combinations.get(0);
            return c.id;
        }
        return null;
    }

    /** 离线保存进货记录 */
    private void savePurchaseOffline(int source, String productName, int quantity, double unitPrice) {
        Long userId = TokenManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "未登录", Toast.LENGTH_SHORT).show();
            btnConfirm.setEnabled(true);
            return;
        }

        double totalAmount = quantity * unitPrice;
        List<LocalPurchaseRecordItem> items = new ArrayList<>();
        LocalPurchaseRecordItem item = new LocalPurchaseRecordItem();
        if (selectedProduct != null) {
            item.productId = selectedProduct.id;
            item.combinationId = null;
        }
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        items.add(item);

        offlineHelper.savePurchaseOffline(userId, source, totalAmount, "", items);

        btnConfirm.setEnabled(true);
        Toast.makeText(this, "入库已保存到本地，网络恢复后自动同步", Toast.LENGTH_LONG).show();
        finish();
    }

    private final Callback<ApiResponse<PurchaseRecord>> purchaseCallback = new Callback<>() {
        @Override
        public void onResponse(Call<ApiResponse<PurchaseRecord>> call, Response<ApiResponse<PurchaseRecord>> response) {
            btnConfirm.setEnabled(true);
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                SoundManager.getInstance(PurchaseActivity.this).playPurchaseSuccess();
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
            if (currentTab == 1 && selectedProduct != null) {
                String qtyStr = etQty.getText().toString().trim();
                String priceStr = etPriceInput.getText().toString().trim();
                int qty = qtyStr.isEmpty() ? 0 : Integer.parseInt(qtyStr);
                double price = priceStr.isEmpty() ? selectedProduct.purchasePrice : Double.parseDouble(priceStr);
                savePurchaseOffline(1, selectedProduct.name, qty, price);
            } else {
                Toast.makeText(PurchaseActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };
}
