package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.network.ApiClient;
import com.heben.client.util.BottomNav;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerProductsActivity extends AppCompatActivity {

    private LinearLayout layoutProducts, layoutCategories;
    private Long sellerId;
    private String sellerName;
    private String currentCategory = "";
    private List<Map<String, Object>> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_products);

        BottomNav.setup(this, R.id.bottom_home);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sellerId = getIntent().hasExtra("sellerId") ? getIntent().getLongExtra("sellerId", 0) : null;
        sellerName = getIntent().getStringExtra("sellerName");

        layoutProducts = findViewById(R.id.layout_products);
        layoutCategories = findViewById(R.id.layout_categories);

        TextView tvShopName = findViewById(R.id.tv_shop_name);
        tvShopName.setText(sellerName != null ? sellerName : "商品选购");

        MaterialButton btnChat = findViewById(R.id.btn_contact_seller);
        MaterialButton btnAppt = findViewById(R.id.btn_make_appointment);
        btnChat.setOnClickListener(v -> openChat());
        btnAppt.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuyerHomeActivity.class);
            startActivity(intent);
        });

        loadProducts();
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        if (sellerId == null) {
            showEmpty("请先搜索并关注店铺");
            return;
        }
        ApiClient.getService().getRetailerProducts(sellerId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call,
                                   Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Map<String, Object>> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        allProducts.clear();
                        allProducts.addAll(data);
                        buildCategories(data);
                        renderProducts(currentCategory);
                    } else {
                        showEmpty("暂无商品");
                    }
                } else {
                    showEmpty("暂无商品");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                showEmpty("网络错误");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void buildCategories(List<Map<String, Object>> products) {
        layoutCategories.removeAllViews();
        TextView tabAll = new TextView(this);
        tabAll.setText("全部");
        tabAll.setTextSize(13);
        tabAll.setPadding(16, 8, 16, 8);
        tabAll.setGravity(android.view.Gravity.CENTER);
        tabAll.setTextColor(ContextCompat.getColor(this, R.color.md_primary));
        tabAll.setBackgroundResource(R.drawable.tab_active_bg);
        tabAll.setTag("");
        tabAll.setOnClickListener(v -> selectCategory(tabAll));
        layoutCategories.addView(tabAll);

        java.util.Set<String> cats = new java.util.LinkedHashSet<>();
        for (Map<String, Object> p : products) {
            Object catObj = p.get("categoryName");
            if (catObj instanceof String && !((String) catObj).isEmpty()) cats.add((String) catObj);
        }
        for (String cat : cats) {
            TextView tab = new TextView(this);
            tab.setText(cat);
            tab.setTextSize(13);
            tab.setPadding(16, 8, 16, 8);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
            tab.setTag(cat);
            tab.setOnClickListener(v -> selectCategory(tab));
            layoutCategories.addView(tab);
        }
    }

    private void selectCategory(TextView selected) {
        currentCategory = (String) selected.getTag();
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            View child = layoutCategories.getChildAt(i);
            if (child instanceof TextView) {
                TextView tab = (TextView) child;
                boolean isSel = tab == selected;
                tab.setTextColor(isSel ? ContextCompat.getColor(this, R.color.md_primary) : ContextCompat.getColor(this, R.color.md_on_surface_variant));
                tab.setBackgroundResource(isSel ? R.drawable.tab_active_bg : 0);
                tab.setTypeface(null, isSel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
        renderProducts(currentCategory);
    }

    @SuppressWarnings("unchecked")
    private void renderProducts(String filter) {
        layoutProducts.removeAllViews();
        for (Map<String, Object> p : allProducts) {
            Object catObj = p.get("categoryName");
            String cat = catObj instanceof String ? (String) catObj : "";
            if (!filter.isEmpty() && !filter.equals(cat)) continue;
            addProductCard(p);
        }
        if (layoutProducts.getChildCount() == 0) {
            showEmpty("暂无此分类商品");
        }
    }

    @SuppressWarnings("unchecked")
    private void addProductCard(Map<String, Object> p) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_product_card, layoutProducts, false);
        String name = (String) p.get("name");
        double price = ((Number) p.get("sellingPrice")).doubleValue();
        ((TextView) card.findViewById(R.id.tv_product_name)).setText(name);
        ((TextView) card.findViewById(R.id.tv_selling_price)).setText("¥" + String.format("%.2f", price));
        List<Map<String, Object>> combos = (List<Map<String, Object>>) p.get("combinations");
        StringBuilder specSb = new StringBuilder();
        if (combos != null) {
            for (Map<String, Object> c : combos) {
                String attrs = (String) c.get("attrsJson");
                if (attrs != null && !attrs.equals("{}") && !attrs.equals("null")) {
                    specSb.append(attrs.replaceAll("[{}\"]", "").replaceAll("\\w+:", "").replace(",", "/")).append(" ");
                }
            }
        }
        String spec = specSb.toString().trim();
        ((TextView) card.findViewById(R.id.tv_product_spec)).setText(spec);
        card.findViewById(R.id.btn_chat).setOnClickListener(v -> openChat());
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layoutProducts.addView(card);
    }

    private void openChat() {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("otherUserId", sellerId);
        intent.putExtra("otherName", sellerName != null ? sellerName : "商家");
        startActivity(intent);
    }

    private void showEmpty(String text) {
        layoutProducts.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 40, 0, 40);
        layoutProducts.addView(tv);
    }
}
