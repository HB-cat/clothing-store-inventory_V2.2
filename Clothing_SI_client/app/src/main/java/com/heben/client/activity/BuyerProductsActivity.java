package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerProductsActivity extends AppCompatActivity {

    private GridLayout layoutProducts;
    private Long sellerId;
    private String sellerName;
    private LinearLayout layoutCategories;
    private String currentCategory = "";
    private List<Map<String, Object>> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_products);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sellerId = getIntent().hasExtra("sellerId") ? getIntent().getLongExtra("sellerId", 0) : null;
        sellerName = getIntent().getStringExtra("sellerName");

        layoutProducts = findViewById(R.id.layout_products);
        layoutCategories = findViewById(R.id.layout_categories);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(sellerName != null ? sellerName : "商品选购");

        // 预约/聊天按钮
        if (sellerId != null) {
            MaterialButton btnAppt = findViewById(R.id.btn_make_appointment);
            MaterialButton btnChat = findViewById(R.id.btn_contact_seller);
            btnAppt.setVisibility(View.VISIBLE);
            btnChat.setVisibility(View.VISIBLE);
            btnAppt.setOnClickListener(v -> {
                Intent intent = new Intent(this, BuyerHomeActivity.class);
                startActivity(intent);
            });
            btnChat.setOnClickListener(v -> openChat());
        }

        // 底部导航
        findViewById(R.id.bottom_home).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerHomeActivity.class)));
        findViewById(R.id.bottom_messages).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerMessagesActivity.class)));
        findViewById(R.id.bottom_profile).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerProfileActivity.class)));

        loadProducts();
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        if (sellerId != null) {
            ApiClient.getService().getRetailerProducts(sellerId).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call,
                                       Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Map<String, Object>> data = response.body().getData();
                        if (data != null) {
                            allProducts.clear();
                            allProducts.addAll(data);
                            buildCategories(data);
                            renderProducts(currentCategory);
                        } else {
                            showEmpty();
                        }
                    } else {
                        showEmpty();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                    showEmpty();
                }
            });
        } else {
            TextView tv = new TextView(this);
            tv.setText("请先搜索并关注店铺");
            tv.setTextSize(13);
            tv.setTextColor(0xFF95A5A6);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(0, 40, 0, 40);
            layoutProducts.addView(tv);
        }
    }

    @SuppressWarnings("unchecked")
    private void buildCategories(List<Map<String, Object>> products) {
        layoutCategories.removeAllViews();
        // "全部" tab
        TextView tabAll = new TextView(this);
        tabAll.setText("全部");
        tabAll.setTextSize(13);
        tabAll.setPadding(16, 0, 16, 0);
        tabAll.setHeight(32);
        tabAll.setGravity(android.view.Gravity.CENTER);
        tabAll.setTextColor(0xFFE85D3A);
        tabAll.setBackgroundResource(R.drawable.tab_active_bg);
        tabAll.setTag("");
        tabAll.setOnClickListener(v -> selectCategory(tabAll));
        layoutCategories.addView(tabAll);

        // 收集所有唯一分类
        java.util.Set<String> cats = new java.util.LinkedHashSet<>();
        for (Map<String, Object> p : products) {
            String name = (String) p.get("name");
            if (name != null && !name.isEmpty()) {
                // Try to extract category from product name or category field
                Object catObj = p.get("category");
                if (catObj instanceof String && !((String) catObj).isEmpty()) {
                    cats.add((String) catObj);
                }
            }
        }
        for (String cat : cats) {
            TextView tab = new TextView(this);
            tab.setText(cat);
            tab.setTextSize(13);
            tab.setPadding(16, 0, 16, 0);
            tab.setHeight(32);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setTextColor(0xFF636E72);
            tab.setTag(cat);
            tab.setOnClickListener(v -> selectCategory(tab));
            layoutCategories.addView(tab);
        }
    }

    private void selectCategory(TextView selected) {
        currentCategory = (String) selected.getTag();
        // Reset all tabs
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            View child = layoutCategories.getChildAt(i);
            if (child instanceof TextView) {
                TextView tab = (TextView) child;
                boolean isSelected = tab == selected;
                tab.setTextColor(isSelected ? 0xFFE85D3A : 0xFF636E72);
                tab.setBackgroundResource(isSelected ? R.drawable.tab_active_bg : 0);
                tab.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
        renderProducts(currentCategory);
    }

    @SuppressWarnings("unchecked")
    private void renderProducts(String categoryFilter) {
        layoutProducts.removeAllViews();
        for (Map<String, Object> p : allProducts) {
            Object catObj = p.get("category");
            String cat = catObj instanceof String ? (String) catObj : "";
            if (!categoryFilter.isEmpty() && !categoryFilter.equals(cat)) continue;
            addProductCard(p);
        }
        if (layoutProducts.getChildCount() == 0) {
            TextView tv = new TextView(this);
            tv.setText("暂无此分类商品");
            tv.setTextSize(13);
            tv.setTextColor(0xFF95A5A6);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(0, 40, 0, 40);
            layoutProducts.addView(tv);
        }
    }

    @SuppressWarnings("unchecked")
    private void addProductCard(Map<String, Object> p) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_product_card, layoutProducts, false);
        if (card == null) return;

        String name = (String) p.get("name");
        double price = ((Number) p.get("sellingPrice")).doubleValue();
        ((TextView) card.findViewById(R.id.tv_product_name)).setText(name);
        ((TextView) card.findViewById(R.id.tv_selling_price)).setText("¥" + String.format("%.2f", price));

        // 规格文本
        List<Map<String, Object>> combos = (List<Map<String, Object>>) p.get("combinations");
        StringBuilder specSb = new StringBuilder();
        if (combos != null && !combos.isEmpty()) {
            for (Map<String, Object> c : combos) {
                String attrs = (String) c.get("attrsJson");
                if (attrs != null && !attrs.equals("{}") && !attrs.equals("null")) {
                    String display = attrs.replaceAll("[{}\"]", "")
                            .replaceAll("\\w+:", "")
                            .replace(",", "/");
                    if (!display.isEmpty()) {
                        specSb.append(display).append(" ");
                    }
                }
            }
        }
        TextView tvSpec = card.findViewById(R.id.tv_product_spec);
        String spec = specSb.toString().trim();
        tvSpec.setText(spec.isEmpty() ? (String) p.get("attrsDisplay") : spec);

        // 咨询按钮
        card.findViewById(R.id.btn_chat).setOnClickListener(v -> openChat());

        // 设置 GridLayout 参数确保2列
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        card.setLayoutParams(lp);

        layoutProducts.addView(card);
    }

    private void openChat() {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("otherUserId", sellerId);
        intent.putExtra("otherName", sellerName != null ? sellerName : "商家");
        startActivity(intent);
    }

    private void showEmpty() {
        layoutProducts.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("暂无商品");
        tv.setTextSize(13);
        tv.setTextColor(0xFF95A5A6);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 40, 0, 40);
        layoutProducts.addView(tv);
    }
}
