package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.SharedProduct;
import com.heben.client.network.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerHomeActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private LinearLayout layoutHot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_home);

        etSearch = findViewById(R.id.et_search);
        layoutHot = findViewById(R.id.layout_hot_products);

        findViewById(R.id.btn_buy).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerProductsActivity.class)));
        findViewById(R.id.bottom_messages).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerMessagesActivity.class)));
        findViewById(R.id.bottom_profile).setOnClickListener(v ->
                startActivity(new Intent(this, BuyerProfileActivity.class)));

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                Intent intent = new Intent(this, BuyerProductsActivity.class);
                intent.putExtra("keyword", etSearch.getText().toString().trim());
                startActivity(intent);
                return true;
            }
            return false;
        });

        loadHotProducts();
    }

    private void loadHotProducts() {
        ApiClient.getService().getAvailableSharedProducts("").enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SharedProduct>>> call, Response<ApiResponse<List<SharedProduct>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SharedProduct> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        layoutHot.removeAllViews();
                        int count = Math.min(data.size(), 2);
                        for (int i = 0; i < count; i++) {
                            SharedProduct p = data.get(i);
                            CardView card = (CardView) LayoutInflater.from(BuyerHomeActivity.this)
                                    .inflate(R.layout.item_shared_product, layoutHot, false);
                            ((TextView) card.findViewById(R.id.tv_product_name)).setText(p.productName);
                            ((TextView) card.findViewById(R.id.tv_product_wholesaler)).setText("商家: " + p.wholesalerName);
                            ((TextView) card.findViewById(R.id.tv_product_price)).setText("¥" + String.format("%.2f", p.sharedPrice));
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                            if (i == 0) lp.setMargins(0, 0, 6, 0);
                            else lp.setMargins(6, 0, 0, 0);
                            card.setLayoutParams(lp);
                            card.findViewById(R.id.btn_wholesale).setOnClickListener(v -> {
                                Intent intent = new Intent(BuyerHomeActivity.this, BuyerProductsActivity.class);
                                startActivity(intent);
                            });
                            layoutHot.addView(card);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<SharedProduct>>> call, Throwable t) {}
        });
    }
}
