package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.User;
import com.heben.client.network.ApiClient;
import com.heben.client.util.BottomNav;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerFollowsActivity extends AppCompatActivity {

    private EditText etSearch;
    private LinearLayout layoutResults, layoutFollowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_follows);

        BottomNav.setup(this, R.id.bottom_follow);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etSearch = findViewById(R.id.et_search);
        layoutResults = findViewById(R.id.layout_search_results);
        layoutFollowed = findViewById(R.id.layout_followed_list);

        findViewById(R.id.btn_search).setOnClickListener(v -> searchShops());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchShops();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFollowed();
    }

    private void searchShops() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) { addResult(layoutResults, "请输入店铺名或关键字"); return; }

        layoutResults.removeAllViews();
        addResult(layoutResults, "搜索中...");

        ApiClient.getService().searchRetailers(keyword).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call,
                                   Response<ApiResponse<List<Map<String, Object>>>> response) {
                layoutResults.removeAllViews();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Map<String, Object>> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        for (Map<String, Object> shop : data) addShopResult(shop);
                    } else {
                        addResult(layoutResults, "未找到相关店铺");
                    }
                } else {
                    addResult(layoutResults, "搜索失败");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                layoutResults.removeAllViews();
                addResult(layoutResults, "网络错误，请检查连接");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addShopResult(Map<String, Object> shop) {
        View card = getLayoutInflater().inflate(R.layout.item_shop_search, layoutResults, false);
        String shopName = (String) shop.get("shopName");
        String nickname = (String) shop.get("nickname");
        Long sellerId = ((Number) shop.get("id")).longValue();
        boolean isFollowing = shop.get("isFollowing") instanceof Boolean
                ? (Boolean) shop.get("isFollowing") : false;

        ((TextView) card.findViewById(R.id.tv_shop_name)).setText(shopName != null ? shopName : nickname);

        MaterialButton btn = card.findViewById(R.id.btn_follow);
        if (isFollowing) {
            btn.setText("已关注");
            btn.setEnabled(false);
        } else {
            btn.setText("+ 关注");
            btn.setOnClickListener(v -> {
                ApiClient.getService().followSeller(sellerId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                        btn.setText("已关注");
                        btn.setEnabled(false);
                        loadFollowed();
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(BuyerFollowsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
        layoutResults.addView(card);
    }

    private void loadFollowed() {
        layoutFollowed.removeAllViews();
        addResult(layoutFollowed, "加载中...");

        ApiClient.getService().getFollowedSellers().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> resp) {
                layoutFollowed.removeAllViews();
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    List<User> users = resp.body().getData();
                    if (users != null && !users.isEmpty()) {
                        for (User u : users) addFollowedItem(u);
                    } else {
                        addResult(layoutFollowed, "暂无关注店铺\n搜索并关注您喜欢的店铺吧");
                    }
                } else {
                    addResult(layoutFollowed, "暂无关注店铺");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                layoutFollowed.removeAllViews();
                addResult(layoutFollowed, "网络错误");
            }
        });
    }

    private void addFollowedItem(User u) {
        View card = getLayoutInflater().inflate(R.layout.item_shop_search, layoutFollowed, false);
        String shopName = u.shopName != null ? u.shopName : u.nickname;
        ((TextView) card.findViewById(R.id.tv_shop_name)).setText(shopName);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuyerProductsActivity.class);
            intent.putExtra("sellerId", u.id);
            intent.putExtra("sellerName", shopName);
            startActivity(intent);
        });
        MaterialButton btn = card.findViewById(R.id.btn_follow);
        btn.setText("取消关注");
        btn.setOnClickListener(v -> {
            ApiClient.getService().unfollowSeller(u.id).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                    loadFollowed();
                }
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Toast.makeText(BuyerFollowsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
        layoutFollowed.addView(card);
    }

    private void addResult(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF95A5A6);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 24, 0, 24);
        tv.setLineSpacing(4, 1);
        parent.addView(tv);
    }
}
